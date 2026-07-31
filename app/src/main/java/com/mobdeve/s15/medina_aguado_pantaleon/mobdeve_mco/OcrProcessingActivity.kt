package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

class OcrProcessingActivity : AppCompatActivity() {
    private lateinit var progressOcr: ProgressBar
    private lateinit var tvOcrTitle: TextView
    private lateinit var tvOcrMessage: TextView
    private lateinit var btnRetryOcr: Button
    private lateinit var btnManualEntry: Button
    private lateinit var btnCancelOcr: Button
    private var imageUriText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ocr_processing_activity)

        progressOcr = findViewById(R.id.progressOcr)
        tvOcrTitle = findViewById(R.id.tvOcrTitle)
        tvOcrMessage = findViewById(R.id.tvOcrMessage)
        btnRetryOcr = findViewById(R.id.btnRetryOcr)
        btnManualEntry = findViewById(R.id.btnManualEntry)
        btnCancelOcr = findViewById(R.id.btnCancelOcr)
        imageUriText = intent.getStringExtra(EXTRA_IMAGE_URI)

        btnRetryOcr.setOnClickListener {
            processReceiptImage()
        }

        btnManualEntry.setOnClickListener {
            openManualEntry()
        }

        btnCancelOcr.setOnClickListener {
            finish()
        }

        processReceiptImage()
    }

    private fun processReceiptImage() {
        val uriText = imageUriText
        if (uriText.isNullOrBlank()) {
            showFailure("No receipt image was selected.")
            return
        }

        showLoading()

        val image = try {
            InputImage.fromFilePath(this, Uri.parse(uriText))
        } catch (exception: Exception) {
            showFailure("Could not read the selected receipt image.")
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
                    putExtra(ReviewReceiptActivity.EXTRA_IMAGE_URI, uriText)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                showFailure("OCR failed. Try again or enter the receipt manually.")
                Toast.makeText(this, "Could not read receipt text.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading() {
        progressOcr.visibility = View.VISIBLE
        btnRetryOcr.visibility = View.GONE
        btnManualEntry.visibility = View.GONE
        tvOcrTitle.text = "Reading Receipt"
        tvOcrMessage.text = "Extracting store name, date, total, and line items."
    }

    private fun showFailure(message: String) {
        progressOcr.visibility = View.GONE
        btnRetryOcr.visibility = View.VISIBLE
        btnManualEntry.visibility = View.VISIBLE
        tvOcrTitle.text = "Could Not Read Receipt"
        tvOcrMessage.text = message
    }

    private fun openManualEntry() {
        val intent = Intent(this, ReviewReceiptActivity::class.java).apply {
            putExtra(ReviewReceiptActivity.EXTRA_IMAGE_URI, imageUriText)
            putExtra(ReviewReceiptActivity.EXTRA_RAW_TEXT, "OCR was skipped or failed.")
            putExtra(ReviewReceiptActivity.EXTRA_STORE_NAME, "")
            putExtra(ReviewReceiptActivity.EXTRA_RECEIPT_DATE, "")
            putExtra(ReviewReceiptActivity.EXTRA_TOTAL_AMOUNT, "")
            putExtra(ReviewReceiptActivity.EXTRA_CATEGORY, "")
            putExtra(ReviewReceiptActivity.EXTRA_ITEMS, "")
        }
        startActivity(intent)
        finish()
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
        const val EXTRA_IMAGE_URI = "extra_image_uri"

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
