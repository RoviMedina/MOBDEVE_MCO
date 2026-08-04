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
        } catch (_: Exception) {
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
        tvOcrTitle.text = getString(R.string.reading_receipt)
        tvOcrMessage.text = getString(R.string.extracting_receipt_data)
    }

    private fun showFailure(message: String) {
        progressOcr.visibility = View.GONE
        btnRetryOcr.visibility = View.VISIBLE
        btnManualEntry.visibility = View.VISIBLE
        tvOcrTitle.text = getString(R.string.could_not_read_receipt)
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
            .map { line -> cleanOcrLine(line) }
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
            items = itemLines.joinToString("\n").ifBlank { "No line items detected" },
        )
    }

    private fun findStoreName(lines: List<String>): String {
        return lines
            .take(8)
            .firstOrNull { line ->
                val normalized = line.lowercase(Locale.US)
                (normalized.length >= 3 &&
                    !noiseKeywords.any { keyword -> normalized.contains(keyword) } &&
                    !datePattern.containsMatchIn(line) &&
                    amountPattern.find(line) == null)
            }
            ?: "Unknown Store"
    }

    private fun findReceiptDate(lines: List<String>): String {
        val labeledDate = lines
            .mapIndexed { index, line -> index to line }
            .firstNotNullOfOrNull { (index, line) ->
                val normalized = line.lowercase(Locale.US)
                if (!dateIssuedKeywords.any { keyword -> normalized.contains(keyword) }) {
                    return@firstNotNullOfOrNull null
                }

                datePattern.find(line)?.value
                    ?: lines.drop(index + 1).take(2).firstNotNullOfOrNull { nextLine ->
                        datePattern.find(nextLine)?.value
                    }
            }

        return labeledDate
            ?: lines.firstNotNullOfOrNull { line ->
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
        val amountNearTotal = prioritizedTotal?.let { (index, line) ->
            pricePattern.findAll(line).lastOrNull()?.value
                ?: lines.drop(index + 1).take(4).firstNotNullOfOrNull { nextLine ->
                    amountPattern.findAll(nextLine).lastOrNull()?.value
                }
        }

        val amount = amountNearTotal
            ?: lines.asReversed().firstNotNullOfOrNull { line ->
                amountPattern.findAll(line).lastOrNull()?.value
            }
            .orEmpty()

        return normalizeAmount(amount)
    }

    private fun findItemLines(lines: List<String>): List<String> {
        val itemLines = mutableListOf<String>()
        var reachedSummary = false

        for (line in lines) {
            val normalized = line.lowercase(Locale.US)
            if (isItemHeader(normalized)) {
                continue
            }

            if (isReceiptSummaryLine(normalized)) {
                reachedSummary = true
                continue
            }

            if (reachedSummary) {
                continue
            }

            val priceMatch = pricePattern.findAll(line).lastOrNull() ?: continue
            val price = normalizeAmount(priceMatch.value).toDoubleOrNull() ?: 0.0
            val itemName = cleanItemName(line.removeRange(priceMatch.range))

            if (isLikelyReceiptItem(itemName, normalized, price)) {
                itemLines.add("$itemName ${normalizeAmount(priceMatch.value)}")
            }
        }

        return itemLines
            .takeIf { it.isNotEmpty() }
            ?: listOf("No line items detected")
    }

    private fun isLikelyReceiptItem(itemName: String, normalizedLine: String, price: Double): Boolean {
        val normalizedName = itemName.lowercase(Locale.US)
        val letterCount = itemName.count { it.isLetter() }
        val digitCount = itemName.count { it.isDigit() }
        val hasLetters = letterCount >= 2
        val hasItemWord = itemKeywords.any { keyword -> normalizedName.contains(keyword) }
        val isMostlyNumbers = digitCount > letterCount
        val hasLongNumber = Regex("""\d{5,}""").containsMatchIn(itemName)
        val isReceiptNoise = noiseKeywords.any { keyword -> normalizedLine.contains(keyword) }
        val isCodeLike = codeKeywords.any { keyword -> normalizedName.contains(keyword) }

        return price > 0.0 &&
            (hasLetters || hasItemWord) &&
            !isMostlyNumbers &&
            !hasLongNumber &&
            !isReceiptNoise &&
            !isCodeLike
    }

    private fun isItemHeader(normalizedLine: String): Boolean {
        return normalizedLine == "item" ||
            normalizedLine == "items" ||
            normalizedLine == "tem" ||
            normalizedLine.contains("qty")
    }

    private fun isReceiptSummaryLine(normalizedLine: String): Boolean {
        return receiptSummaryKeywords.any { keyword -> normalizedLine.contains(keyword) } ||
            normalizedLine.startsWith("sales") ||
            normalizedLine.startsWith("less") ||
            normalizedLine.startsWith("vatable") ||
            normalizedLine.startsWith("vat exempt") ||
            normalizedLine.startsWith("zero rated") ||
            normalizedLine.startsWith("cash tendered") ||
            normalizedLine == "change"
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
            .replace("\u20B1", "")
            .removePrefix("P")
            .replace("$", "")
            .trim()
    }

    private fun cleanOcrLine(rawLine: String): String {
        return rawLine
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("""\b[uv][ae]t[_\s-]*reg\b""", RegexOption.IGNORE_CASE), "VAT REG")
            .replace(Regex("""\bt[!1l|]n\b""", RegexOption.IGNORE_CASE), "TIN")
            .replace(Regex("""\b(?:dale|daie|dafe)\b""", RegexOption.IGNORE_CASE), "Date")
            .replace(Regex("""\b(?:iqtal|iotal|1otal|tota[1l])\b""", RegexOption.IGNORE_CASE), "Total")
            .replace(Regex("""\bja[kx]e[-\s]*qut\b""", RegexOption.IGNORE_CASE), "Take-Out")
            .replace(Regex("""\b(?:inv[q0]ice|lnvoice|invo1ce)\b""", RegexOption.IGNORE_CASE), "Invoice")
            .replace(Regex("""\b(?:seriel|sene[l1])\b""", RegexOption.IGNORE_CASE), "Serial")
            .replace(Regex("""\bsuruey\b""", RegexOption.IGNORE_CASE), "Survey")
            .replace(Regex("""\brece[!1]pt\b""", RegexOption.IGNORE_CASE), "Receipt")
            .trim()
    }

    private fun cleanItemName(rawName: String): String {
        return rawName
            .trim(' ', '-', ':', '.', '\t')
            .replace(Regex("""^[sS]?\d+\s*"""), "")
            .replace(Regex("""(?<=[a-z])(?=[A-Z])"""), " ")
            .replace(Regex("""\bch[1!l|]cken\b""", RegexOption.IGNORE_CASE), "Chicken")
            .replace(Regex("""\bfr[1!l|]es\b""", RegexOption.IGNORE_CASE), "Fries")
            .replace(Regex("""\b(?:burgcr|burqer)\b""", RegexOption.IGNORE_CASE), "Burger")
            .replace(Regex("""\bc0ffee\b""", RegexOption.IGNORE_CASE), "Coffee")
            .replace(Regex("""\bdr[1!l|]nk\b""", RegexOption.IGNORE_CASE), "Drink")
            .replace(Regex("""\bcre\s*al\b""", RegexOption.IGNORE_CASE), "Cream")
            .replace(Regex("""(?<=\p{Alpha})0(?=\p{Alpha})"""), "O")
            .replace(Regex("""(?<=\p{Alpha})1(?=\p{Alpha})"""), "I")
            .replace(Regex("""(?<=\p{Alpha})5(?=\p{Alpha})"""), "S")
            .replace(Regex("""\s+\d{1,2}$"""), "")
            .replace(Regex("\\s+"), " ")
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

        private val amountPattern = Regex("""(?:PHP|P|\u20B1|\$)\s*\d+(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:,\d{3})*\.\d{1,2}""")
        private val pricePattern = Regex("""(?:PHP|P|\u20B1|\$)\s*\d+(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:,\d{3})*\.\d{1,2}|\b\d{1,4}\b$""")
        private val datePattern = Regex(
            """\b(?:\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{4}[./-]\d{1,2}[./-]\d{1,2}|[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})\b"""
        )
        private val dateIssuedKeywords = listOf(
            "date issued",
            "issue date",
            "issued",
            "or date",
            "receipt date",
            "date"
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
        private val receiptSummaryKeywords = totalKeywords + subtotalKeywords + listOf("tax", "discount")
        private val noiseKeywords = listOf(
            "official receipt",
            "receipt",
            "invoice",
            "inv",
            "tin",
            "t!n",
            "vat",
            "vat reg",
            "vat_reg",
            "address",
            "date",
            "tel",
            "telephone",
            "phone",
            "online delivery",
            "delivery",
            "visit",
            "experience",
            "tell us",
            "cashier",
            "transaction",
            "reference",
            "ref",
            "thank you",
            "thanks",
            "www.",
            ".com",
            "survey",
            "serial",
            "seriel",
            "pcno",
            "permit",
            "terminal",
            "machine",
            "accreditation",
            "min",
            "bir",
            "order no",
            "control no"
        )
        private val codeKeywords = listOf(
            "code",
            "invoice",
            "inv",
            "tin",
            "t!n",
            "date",
            "serial",
            "seriel",
            "pcno",
            "id",
            "permit",
            "reference",
            "ref",
            "telephone",
            "tel",
            "phone"
        )
        private val itemKeywords = listOf(
            "chicken",
            "burger",
            "fries",
            "rice",
            "meal",
            "drink",
            "coffee",
            "tea",
            "milk",
            "bread",
            "water",
            "juice",
            "spaghetti",
            "sandwich",
            "pizza",
            "latte",
            "cream",
            "frappe",
            "mcflurry",
            "notebook",
            "pen",
            "book",
            "fare"
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
