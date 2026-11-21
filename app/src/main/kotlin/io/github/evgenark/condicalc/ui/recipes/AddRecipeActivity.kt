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
import io.github.evgenark.condicalc.data.Ingredient
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
    private var editRecipeId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)
        totalText = findViewById(R.id.totalText)

        editRecipeId = intent.getLongExtra("recipe_id", 0L)
        if (editRecipeId > 0) title = "Редактирование рецепта"

        val list = findViewById<RecyclerView>(R.id.ingredientEditList)
        adapter = RowAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        // Первую строку добавляем НЕ здесь, а после загрузки справочников —
        // и только при создании рецепта

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

        if (editRecipeId > 0) {
            val recipe = Repo.getRecipeById(editRecipeId) ?: return
            findViewById<EditText>(R.id.recipeNameInput).setText(recipe.name)
            rows.clear()
            recipe.ingredients.forEach { ing ->
                rows.add(Row().apply {
                    productId = ing.product.id
                    measure = ing.measure
                    amountText = if (ing.amount % 1.0 == 0.0)
                        ing.amount.toInt().toString()
                    else
                        "%.1f".format(ing.amount)
                })
            }
        } else {
            rows.add(Row())
        }

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
            Repo.saveRecipe(name, items, editRecipeId)
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
                    else -> null
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

            var row: Row? = null
            var lastProductPos = -1   // позиция, выставленная программно
            var lastMeasurePos = -1

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
                        if (pos == lastProductPos) return   // ложный вызов после setSelection — игнорируем
                        lastProductPos = pos
                        if (pos in products.indices) row?.productId = products[pos].id
                        updateTotal()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                measureSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        if (pos == lastMeasurePos) return
                        lastMeasurePos = pos
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
            holder.row = row

            // Свежий адаптер на каждый биндинг — иначе холдер, созданный до загрузки
            // справочников, навсегда остаётся с пустым списком продуктов
            holder.productSpinner.adapter = ArrayAdapter(
                this@AddRecipeActivity,
                android.R.layout.simple_spinner_dropdown_item,
                products.map { it.name }
            )
            val productPos = products.indexOfFirst { it.id == row.productId }
            val selProduct = if (productPos >= 0) productPos else 0
            holder.lastProductPos = selProduct
            holder.productSpinner.setSelection(selProduct, false)

            holder.measureSpinner.adapter = ArrayAdapter(
                this@AddRecipeActivity,
                android.R.layout.simple_spinner_dropdown_item,
                measureNames.map { if (it.isEmpty()) "— (шт, г, кг)" else it }
            )
            val measurePos = measureNames.indexOf(row.measure)
            val selMeasure = if (measurePos >= 0) measurePos else 0
            holder.lastMeasurePos = selMeasure
            holder.measureSpinner.setSelection(selMeasure, false)

            holder.amountInput.setText(row.amountText)
        }
    }
}
