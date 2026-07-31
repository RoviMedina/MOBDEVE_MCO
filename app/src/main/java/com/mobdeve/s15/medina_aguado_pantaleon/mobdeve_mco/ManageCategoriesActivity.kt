package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Context
import android.graphics.Color
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
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var etNewCategory: TextInputEditText
    private lateinit var tilNewCategory: TextInputLayout
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_categories_activity)

        etNewCategory = findViewById(R.id.etNewCategory)
        tilNewCategory = findViewById(R.id.tilNewCategory)

        categoryAdapter = CategoryAdapter(
            categories,
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

        categories.add(Category(name, categoryColors[categories.size % categoryColors.size]))
        saveCategories()
        categoryAdapter.replaceItems(categories)
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

                categories[position] = category.copy(name = newName)
                saveCategories()
                categoryAdapter.replaceItems(categories)
            }
            .show()
    }

    private fun confirmDelete(position: Int) {
        val category = categories.getOrNull(position) ?: return

        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Delete ${category.name}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                categories.removeAt(position)
                saveCategories()
                categoryAdapter.replaceItems(categories)
            }
            .show()
    }

    private fun loadCategories() {
        val savedCategories = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CATEGORIES, null)

        categories.clear()
        categories.addAll(
            savedCategories
                ?.lines()
                ?.mapNotNull { line -> parseCategory(line) }
                ?.takeIf { it.isNotEmpty() }
                ?: defaultCategories
        )
        categoryAdapter.replaceItems(categories)
    }

    private fun saveCategories() {
        val value = categories.joinToString("\n") { category ->
            "${category.name}|${category.color}"
        }

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CATEGORIES, value)
            .apply()
    }

    private fun parseCategory(line: String): Category? {
        val parts = line.split("|")
        if (parts.size != 2) {
            return null
        }

        val name = parts[0].trim()
        val color = parts[1].toIntOrNull() ?: return null

        return if (name.isBlank()) null else Category(name, color)
    }

    companion object {
        private const val PREFS_NAME = "categories"
        private const val KEY_CATEGORIES = "category_list"

        private val categoryColors = listOf(
            Color.rgb(103, 80, 164),
            Color.rgb(46, 125, 50),
            Color.rgb(2, 119, 189),
            Color.rgb(239, 108, 0),
            Color.rgb(198, 40, 40)
        )

        private val defaultCategories = listOf(
            Category("Food", categoryColors[0]),
            Category("Transportation", categoryColors[1]),
            Category("School", categoryColors[2]),
            Category("Coffee", categoryColors[3])
        )
    }
}
