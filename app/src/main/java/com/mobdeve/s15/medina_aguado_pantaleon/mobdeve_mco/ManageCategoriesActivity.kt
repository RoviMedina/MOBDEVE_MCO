package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.os.Bundle
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
    private lateinit var etNewCategory: TextInputEditText
    private lateinit var tilNewCategory: TextInputLayout
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_categories_activity)

        receiptDatabaseHelper = ReceiptDatabaseHelper(this)
        etNewCategory = findViewById(R.id.etNewCategory)
        tilNewCategory = findViewById(R.id.tilNewCategory)

        categoryAdapter = CategoryAdapter(
            categories,
            onColorClick = { position -> changeCategoryColor(position) },
            onEditClick = { position -> showRenameDialog(position) },
            onDeleteClick = { position -> confirmDelete(position) }
        )

        findViewById<RecyclerView>(R.id.rvCategories).apply {
            layoutManager = LinearLayoutManager(this@ManageCategoriesActivity)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }

        findViewById<MaterialButton>(R.id.btnAddCategory).setOnClickListener {
            addCategory()
        }

        loadCategories()
    }

    private fun addCategory() {
        val name = etNewCategory.text.toString().trim()
        tilNewCategory.error = null

        if (name.isBlank()) {
            tilNewCategory.error = "Category name is required"
            return
        }

        if (categories.any { it.name.equals(name, ignoreCase = true) }) {
            tilNewCategory.error = "Category already exists"
            return
        }

        val color = ReceiptDatabaseHelper.categoryColors[categories.size % ReceiptDatabaseHelper.categoryColors.size]
        receiptDatabaseHelper.insertCategory(name, color)
        loadCategories()
        etNewCategory.text?.clear()
    }

    private fun showRenameDialog(position: Int) {
        val category = categories.getOrNull(position) ?: return
        val input = TextInputEditText(this)
        input.setText(category.name)
        input.setSingleLine(true)

        AlertDialog.Builder(this)
            .setTitle("Rename Category")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isBlank()) {
                    Toast.makeText(this, "Category name is required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val duplicateExists = categories.withIndex().any { indexedCategory ->
                    indexedCategory.index != position &&
                        indexedCategory.value.name.equals(newName, ignoreCase = true)
                }

                if (duplicateExists) {
                    Toast.makeText(this, "Category already exists.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                receiptDatabaseHelper.updateCategory(category.id, newName, category.color)
                loadCategories()
            }
            .show()
    }

    private fun changeCategoryColor(position: Int) {
        val category = categories.getOrNull(position) ?: return
        val colors = ReceiptDatabaseHelper.categoryColors
        val currentIndex = colors.indexOf(category.color).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle("Choose Color")
            .setSingleChoiceItems(colorNames, currentIndex) { dialog, selectedIndex ->
                receiptDatabaseHelper.updateCategory(category.id, category.name, colors[selectedIndex])
                loadCategories()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(position: Int) {
        val category = categories.getOrNull(position) ?: return

        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Delete ${category.name}? Receipts using this category will be moved to None.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                receiptDatabaseHelper.deleteCategory(category.id)
                loadCategories()
            }
            .show()
    }

    private fun loadCategories() {
        categories.clear()
        categories.addAll(receiptDatabaseHelper.getAllCategories())
        categoryAdapter.replaceItems(categories)
    }

    companion object {
        private val colorNames = arrayOf("Purple", "Green", "Blue", "Orange", "Red")
    }
}
