package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ManageCategoriesActivity : AppCompatActivity() {
    private lateinit var receiptDatabaseHelper: ReceiptDatabaseHelper
    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_categories_activity)

        receiptDatabaseHelper = ReceiptDatabaseHelper(this)

        categoryAdapter = CategoryAdapter(
            categories,
            onCategoryClick = { position -> showCategoryActionsDialog(position) }
        )

        findViewById<RecyclerView>(R.id.rvCategories).apply {
            layoutManager = LinearLayoutManager(this@ManageCategoriesActivity)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }

        findViewById<MaterialButton>(R.id.btnAddCategory).setOnClickListener {
            showAddCategoryDialog()
        }

        loadCategories()
    }

    private fun showAddCategoryDialog() {
        val views = createCategoryDialogViews()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(views.container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = views.nameInput.text.toString().trim()
                if (!isCategoryNameValid(name, null)) {
                    return@setOnClickListener
                }

                val color = ReceiptDatabaseHelper.categoryColors[views.colorSpinner.selectedItemPosition]
                receiptDatabaseHelper.insertCategory(name, color)
                loadCategories()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showCategoryActionsDialog(position: Int) {
        val category = categories.getOrNull(position) ?: return
        val currentColorIndex = ReceiptDatabaseHelper.categoryColors.indexOf(category.color)
            .takeIf { it >= 0 }
            ?: 0
        val views = createCategoryDialogViews(category.name, currentColorIndex)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Category")
            .setView(views.container)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = views.nameInput.text.toString().trim()
                if (!isCategoryNameValid(newName, category.id)) {
                    return@setOnClickListener
                }

                val newColor = ReceiptDatabaseHelper.categoryColors[views.colorSpinner.selectedItemPosition]
                receiptDatabaseHelper.updateCategory(category.id, newName, newColor)
                loadCategories()
                dialog.dismiss()
            }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                confirmDelete(category, dialog)
            }
        }

        dialog.show()
    }

    private fun confirmDelete(category: Category, editDialog: AlertDialog) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Delete ${category.name}? Receipts using this category will be moved to None.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                receiptDatabaseHelper.deleteCategory(category.id)
                loadCategories()
                editDialog.dismiss()
            }
            .show()
    }

    private fun createCategoryDialogViews(
        categoryName: String = "",
        selectedColorIndex: Int = 0
    ): CategoryDialogViews {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        val nameLayout = TextInputLayout(this).apply {
            hint = "Category Name"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_FILLED
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val nameInput = TextInputEditText(nameLayout.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine(true)
            setText(categoryName)
        }

        val colorSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@ManageCategoriesActivity,
                android.R.layout.simple_spinner_item,
                colorNames
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(selectedColorIndex)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }

        nameLayout.addView(nameInput)
        container.addView(nameLayout)
        container.addView(colorSpinner)

        return CategoryDialogViews(container, nameInput, colorSpinner)
    }

    private fun isCategoryNameValid(name: String, editingCategoryId: Long?): Boolean {
        if (name.isBlank()) {
            Toast.makeText(this, "Category name is required.", Toast.LENGTH_SHORT).show()
            return false
        }

        val duplicateExists = categories.any { category ->
            category.id != editingCategoryId && category.name.equals(name, ignoreCase = true)
        }

        if (duplicateExists) {
            Toast.makeText(this, "Category already exists.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun loadCategories() {
        categories.clear()
        categories.addAll(receiptDatabaseHelper.getAllCategories())
        categoryAdapter.replaceItems(categories)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class CategoryDialogViews(
        val container: LinearLayout,
        val nameInput: TextInputEditText,
        val colorSpinner: Spinner
    )

    companion object {
        private val colorNames = arrayOf("Purple", "Green", "Blue", "Orange", "Red")
    }
}
