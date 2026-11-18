package io.github.evgenark.condicalc.ui.recipes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Product
import io.github.evgenark.condicalc.data.Repo
import kotlinx.coroutines.launch

class AddRecipeActivity : AppCompatActivity() {

    private class Row {
        var productId: Long = -1L
        var amountText: String = ""
        var measure: String = ""   // "" — в единицах продукта
    }

    private val products = mutableListOf<Product>()
    private val measureNames = mutableListOf<String>()
    private val rows = mutableListOf<Row>()
    private var adapter: RowAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)

        val list = findViewById<RecyclerView>(R.id.ingredientEditList)
        adapter = RowAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        rows.add(Row()) // первая строка сразу

        findViewById<Button>(R.id.addIngredientBtn).setOnClickListener {
            rows.add(Row())
            adapter?.notifyDataSetChanged()
        }
        findViewById<Button>(R.id.saveRecipeBtn).setOnClickListener { save() }

        lifecycleScope.launch { loadDictionaries() }
    }

    private suspend fun loadDictionaries() {
        products.clear()
        products.addAll(Repo.getAllProducts())

        measureNames.clear()
        measureNames.add("") // «—» = в единицах продукта
        measureNames.addAll(Repo.getAllConversions().map { it.name })

        adapter?.notifyDataSetChanged()
    }

    private fun save() {
        val name = findViewById<EditText>(R.id.recipeNameInput).text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название рецепта", Toast.LENGTH_SHORT).show()
            return
        }
        if (products.isEmpty()) {
            Toast.makeText(this, "Сначала добавьте продукты", Toast.LENGTH_SHORT).show()
            return
        }

        val items = rows.mapNotNull { row ->
            val amount = row.amountText.replace(',', '.').toDoubleOrNull()
            if (row.productId > 0 && amount != null && amount > 0)
                Triple(row.productId, amount, row.measure)
            else null
        }
        if (items.isEmpty()) {
            Toast.makeText(this, "Заполните хотя бы одну строку: продукт и количество", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            Repo.saveRecipe(name, items)
            finish()
        }
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val productSpinner: Spinner = v.findViewById(R.id.productSpinner)
            val amountInput: EditText = v.findViewById(R.id.amountInput)
            val measureSpinner: Spinner = v.findViewById(R.id.measureSpinner)
            val deleteBtn: TextView = v.findViewById(R.id.deleteRowBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ingredient_edit, parent, false)
        )

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]

            // --- Продукт ---
            val productAdapter = ArrayAdapter(
                this@AddRecipeActivity,
                android.R.layout.simple_spinner_dropdown_item,
                products.map { it.name }
            )
            holder.productSpinner.adapter = productAdapter
            val productPos = products.indexOfFirst { it.id == row.productId }
            holder.productSpinner.setSelection(if (productPos >= 0) productPos else 0, false)
            holder.productSpinner.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        if (pos in products.indices) row.productId = products[pos].id
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            // --- Мера ---
            val measureAdapter = ArrayAdapter(
                this@AddRecipeActivity,
                android.R.layout.simple_spinner_dropdown_item,
                measureNames.map { if (it.isEmpty()) "— (шт, г, кг)" else it }
            )
            holder.measureSpinner.adapter = measureAdapter
            val measurePos = measureNames.indexOf(row.measure)
            holder.measureSpinner.setSelection(if (measurePos >= 0) measurePos else 0, false)
            holder.measureSpinner.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        if (pos in measureNames.indices) row.measure = measureNames[pos]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            // --- Количество ---
            holder.amountInput.setText(row.amountText)
            if (holder.amountInput.tag == null) {
                holder.amountInput.tag = true
                holder.amountInput.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        row.amountText = s?.toString() ?: ""
                    }
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                })
            }

            // --- Удаление строки ---
            holder.deleteBtn.setOnClickListener {
                if (rows.size > 1) {
                    rows.removeAt(position)
                    adapter?.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@AddRecipeActivity, "Нужен хотя бы один ингредиент", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
