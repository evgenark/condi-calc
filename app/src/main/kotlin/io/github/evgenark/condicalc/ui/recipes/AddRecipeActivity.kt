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
import io.github.evgenark.condicalc.data.Unit
import io.github.evgenark.condicalc.data.db.UnitConversionEntity
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
    private var convByName: Map<String, UnitConversionEntity> = emptyMap()
    private var refsByPair: Map<Pair<Long, Long>, Double> = emptyMap()
    private lateinit var totalText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)
        totalText = findViewById(R.id.totalText)

        val list = findViewById<RecyclerView>(R.id.ingredientEditList)
        adapter = RowAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        rows.add(Row()) // первая строка сразу

        findViewById<Button>(R.id.addIngredientBtn).setOnClickListener {
            rows.add(Row())
            adapter?.notifyItemInserted(rows.size - 1)
            updateTotal()
        }
        findViewById<Button>(R.id.saveRecipeBtn).setOnClickListener { save() }

        lifecycleScope.launch { loadDictionaries() }
    }

    private suspend fun loadDictionaries() {
        products.clear()
        products.addAll(Repo.getAllProducts())

        measureNames.clear()
        measureNames.add("")
        measureNames.addAll(Repo.getAllConversions().map { it.name })

        convByName = Repo.getAllConversions().associateBy { it.name.lowercase() }
        refsByPair = Repo.getAllRefs().associate { (it.productId to it.conversionId) to it.quantity }

        adapter?.notifyDataSetChanged()
        updateTotal()
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

    private fun updateTotal() {
        var total = 0.0
        for (row in rows) {
            val product = products.firstOrNull { it.id == row.productId } ?: continue
            val amount = row.amountText.replace(',', '.').toDoubleOrNull() ?: continue

            val ing = Ingredient(product = product, amount = amount, measure = row.measure)
            if (row.measure.isNotEmpty()) {
                val conv = convByName[row.measure.lowercase()] ?: continue
                val perMeasure = refsByPair[row.productId to conv.id] ?: conv.grams
                ing.grams = when (product.unit) {
                    Unit.G, Unit.KG, Unit.L, Unit.ML -> amount * perMeasure
                    else -> null   // штучные — считаем только в единицах продукта
                }
            }
            total += ing.cost
        }
        totalText.text = "Итого: %.2f ₽".format(total)
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val productSpinner: Spinner = v.findViewById(R.id.productSpinner)
            val amountInput: EditText = v.findViewById(R.id.amountInput)
            val measureSpinner: Spinner = v.findViewById(R.id.measureSpinner)
            val deleteBtn: TextView = v.findViewById(R.id.deleteRowBtn)

            var row: Row? = null  // строка, к которой холдер привязан СЕЙЧАС

            init {
                amountInput.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        row?.amountText = s?.toString() ?: ""
                        updateTotal()
                    }
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                })

                productSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        if (pos in products.indices) row?.productId = products[pos].id
                        updateTotal()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                measureSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        if (pos in measureNames.indices) row?.measure = measureNames[pos]
                        updateTotal()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                deleteBtn.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION && rows.size > 1) {
                        rows.removeAt(pos)
                        notifyItemRemoved(pos)
                        updateTotal()
                    } else if (rows.size <= 1) {
                        Toast.makeText(this@AddRecipeActivity, "Нужен хотя бы один ингредиент", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ingredient_edit, parent, false)
        )

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]

            // СНАЧАЛА перепривязываем холдер к строке, ПОТОМ трогаем view —
            // иначе setText/setSelection вызовут лисенер, и он запишет значение в чужую строку
            holder.row = row

            // --- Продукт ---
            holder.productSpinner.adapter = productAdapterFor(holder)
            val productPos = products.indexOfFirst { it.id == row.productId }
            holder.productSpinner.setSelection(if (productPos >= 0) productPos else 0, false)

            // --- Мера ---
            holder.measureSpinner.adapter = measureAdapterFor(holder)
            val measurePos = measureNames.indexOf(row.measure)
            holder.measureSpinner.setSelection(if (measurePos >= 0) measurePos else 0, false)

            // --- Количество ---
            holder.amountInput.setText(row.amountText)
        }

        private fun productAdapterFor(holder: VH): ArrayAdapter<String> {
            if (holder.productSpinner.adapter == null) {
                holder.productSpinner.adapter = ArrayAdapter(
                    this@AddRecipeActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    products.map { it.name }
                )
            }
            return holder.productSpinner.adapter as ArrayAdapter<String>
        }

        private fun measureAdapterFor(holder: VH): ArrayAdapter<String> {
            if (holder.measureSpinner.adapter == null) {
                holder.measureSpinner.adapter = ArrayAdapter(
                    this@AddRecipeActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    measureNames.map { if (it.isEmpty()) "— (шт, г, кг)" else it }
                )
            }
            return holder.measureSpinner.adapter as ArrayAdapter<String>
        }
    }
}
