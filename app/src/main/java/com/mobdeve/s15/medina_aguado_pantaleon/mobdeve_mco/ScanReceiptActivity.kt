package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ScanReceiptActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var tvScanStatus: TextView
    private var imageCapture: ImageCapture? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                tvScanStatus.text = "Camera permission is needed to scan receipts."
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                processReceiptImage(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_receipt_activity)

        previewView = findViewById(R.id.previewView)
        tvScanStatus = findViewById(R.id.tvScanStatus)

        findViewById<Button>(R.id.btnCaptureReceipt).setOnClickListener {
            captureReceipt()
        }

        findViewById<Button>(R.id.btnUploadReceipt).setOnClickListener {
            pickImage.launch("image/*")
        }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                tvScanStatus.text = "Place the receipt inside the frame, then capture or upload an image."
            } catch (exception: Exception) {
                tvScanStatus.text = "Unable to start camera preview."
                Toast.makeText(this, "Camera failed to start.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureReceipt() {
        val capture = imageCapture
        if (capture == null) {
            Toast.makeText(this, "Camera is still starting.", Toast.LENGTH_SHORT).show()
            return
        }

        tvScanStatus.text = "Capturing receipt..."

        val photoFile = File(
            cacheDir,
            "receipt_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    processReceiptImage(Uri.fromFile(photoFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    tvScanStatus.text = "Capture failed. Please try again."
                    Toast.makeText(this@ScanReceiptActivity, "Could not capture receipt.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun processReceiptImage(uri: Uri) {
        tvScanStatus.text = "Reading receipt text..."

        val image = try {
            InputImage.fromFilePath(this, uri)
        } catch (exception: Exception) {
            Toast.makeText(this, "Could not read selected image.", Toast.LENGTH_SHORT).show()
            tvScanStatus.text = "Select a clearer receipt image."
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val parsedReceipt = parseReceiptText(visionText.text)
                val intent = Intent(this, ReviewReceiptActivity::class.java).apply {
                    putExtra(ReviewReceiptActivity.EXTRA_STORE_NAME, parsedReceipt.storeName)
                    putExtra(ReviewReceiptActivity.EXTRA_RECEIPT_DATE, parsedReceipt.date)
                    putExtra(ReviewReceiptActivity.EXTRA_TOTAL_AMOUNT, parsedReceipt.totalAmount)
                    putExtra(ReviewReceiptActivity.EXTRA_CATEGORY, parsedReceipt.category)
                    putExtra(ReviewReceiptActivity.EXTRA_ITEMS, parsedReceipt.items)
                    putExtra(ReviewReceiptActivity.EXTRA_RAW_TEXT, visionText.text)
                    putExtra(ReviewReceiptActivity.EXTRA_IMAGE_URI, uri.toString())
                }
                startActivity(intent)
            }
            .addOnFailureListener {
                tvScanStatus.text = "OCR failed. You can try again or enter details manually."
                Toast.makeText(this, "Could not read receipt text.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseReceiptText(rawText: String): ParsedReceipt {
        val lines = rawText.lines()
            .map { line -> line.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotEmpty() }

        val storeName = findStoreName(lines)
        val date = findReceiptDate(lines)
        val totalAmount = findTotalAmount(lines)
        val itemLines = findItemLines(lines)
        val category = predictCategory(lines)

        return ParsedReceipt(
            storeName = storeName,
            date = date.ifBlank { "Date not detected" },
            totalAmount = totalAmount.ifBlank { "0.00" },
            category = category,
            items = itemLines.joinToString("\n").ifBlank { "No line items detected" }
        )
    }

    private fun findStoreName(lines: List<String>): String {
        return lines
            .take(8)
            .firstOrNull { line ->
                val normalized = line.lowercase(Locale.US)
                normalized.length >= 3 &&
                    !noiseKeywords.any { keyword -> normalized.contains(keyword) } &&
                    !datePattern.containsMatchIn(line) &&
                    amountPattern.find(line) == null
            }
            ?: "Unknown Store"
    }

    private fun findReceiptDate(lines: List<String>): String {
        return lines.firstNotNullOfOrNull { line ->
            datePattern.find(line)?.value
        }.orEmpty()
    }

    private fun findTotalAmount(lines: List<String>): String {
        val prioritizedTotal = lines
            .mapIndexed { index, line -> index to line }
            .filter { (_, line) ->
                val normalized = line.lowercase(Locale.US)
                totalKeywords.any { keyword -> normalized.contains(keyword) } &&
                    !subtotalKeywords.any { keyword -> normalized.contains(keyword) }
            }
            .lastOrNull()
            ?.second

        val amount = amountPattern.findAll(prioritizedTotal.orEmpty()).lastOrNull()?.value
            ?: lines.asReversed().firstNotNullOfOrNull { line ->
                amountPattern.findAll(line).lastOrNull()?.value
            }
            .orEmpty()

        return normalizeAmount(amount)
    }

    private fun findItemLines(lines: List<String>): List<String> {
        val itemLines = lines.filter { line ->
            val normalized = line.lowercase(Locale.US)
            val hasAmount = amountPattern.containsMatchIn(line)
            val isReceiptSummary = receiptSummaryKeywords.any { keyword -> normalized.contains(keyword) }
            val isReceiptNoise = noiseKeywords.any { keyword -> normalized.contains(keyword) }

            hasAmount && !isReceiptSummary && !isReceiptNoise
        }

        return itemLines
            .takeIf { it.isNotEmpty() }
            ?: lines.filterNot { line -> noiseKeywords.any { keyword -> line.lowercase(Locale.US).contains(keyword) } }
                .take(8)
    }

    private fun predictCategory(lines: List<String>): String {
        val receiptText = lines.joinToString(" ").lowercase(Locale.US)
        return categoryKeywords.firstOrNull { (_, keywords) ->
            keywords.any { keyword -> receiptText.contains(keyword) }
        }?.first ?: "Uncategorized"
    }

    private fun normalizeAmount(rawAmount: String): String {
        return rawAmount
            .replace(",", "")
            .replace("PHP", "", ignoreCase = true)
            .removePrefix("P")
            .trim()
    }

    private data class ParsedReceipt(
        val storeName: String,
        val date: String,
        val totalAmount: String,
        val category: String,
        val items: String
    )

    companion object {
        private val amountPattern = Regex("""(?:PHP|P)?\s*\d{1,3}(?:,\d{3})*(?:\.\d{2})?|\d+(?:\.\d{2})""")
        private val datePattern = Regex(
            """\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}[/-]\d{1,2}[/-]\d{1,2}|[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})\b"""
        )

        private val totalKeywords = listOf(
            "grand total",
            "amount due",
            "balance due",
            "total due",
            "total amount",
            "total"
        )
        private val subtotalKeywords = listOf("subtotal", "sub total", "change", "cash", "card", "visa", "mastercard")
        private val receiptSummaryKeywords = totalKeywords + subtotalKeywords + listOf("vat", "tax", "discount")
        private val noiseKeywords = listOf(
            "official receipt",
            "receipt",
            "invoice",
            "tin",
            "vat reg",
            "address",
            "tel",
            "phone",
            "cashier",
            "transaction",
            "reference",
            "thank you",
            "thanks",
            "www.",
            ".com"
        )
        private val categoryKeywords = listOf(
            "Food" to listOf(
                "jollibee",
                "mcdonald",
                "burger",
                "chicken",
                "pizza",
                "restaurant",
                "cafe",
                "coffee",
                "starbucks",
                "food",
                "meal",
                "drink"
            ),
            "Groceries" to listOf(
                "grocery",
                "supermarket",
                "puregold",
                "sm supermarket",
                "savemore",
                "waltermart",
                "robinsons supermarket",
                "market",
                "produce"
            ),
            "Transportation" to listOf(
                "grab",
                "taxi",
                "fare",
                "transport",
                "gas",
                "fuel",
                "parking",
                "toll",
                "ride"
            ),
            "School" to listOf(
                "bookstore",
                "national bookstore",
                "school",
                "tuition",
                "notebook",
                "pen",
                "book",
                "supplies"
            ),
            "Utilities" to listOf(
                "meralco",
                "maynilad",
                "water",
                "electric",
                "internet",
                "wifi",
                "bill",
                "utility"
            ),
            "Health" to listOf(
                "pharmacy",
                "drugstore",
                "mercury drug",
                "watsons",
                "clinic",
                "hospital",
                "medicine"
            )
        )
    }
}
