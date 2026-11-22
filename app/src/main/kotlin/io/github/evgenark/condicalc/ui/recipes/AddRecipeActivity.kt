package io.github.evgenark.condicalc.ui.recipes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Filter
import android.widget.Filter.FilterResults
import android.widget.ImageView
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
        var costLabel: String = "" // то, что показываем справа на нижнем этаже
    }

    private data class MeasureOption(val label: String, val value: String, val enabled: Boolean = true)

    private val products = mutableListOf<Product>()
    private val conversions = mutableListOf<UnitConversionEntity>()
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

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.addIngredientBtn).setOnClickListener {
            rows.add(Row())
            adapter?.notifyItemInserted(rows.size - 1)
            updateTotal()
        }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.saveRecipeBtn).setOnClickListener { save() }

        lifecycleScope.launch { loadDictionaries() }
    }

    private suspend fun loadDictionaries() {
        products.clear()
        products.addAll(Repo.getAllProducts())

        conversions.clear()
        conversions.addAll(Repo.getAllConversions())
        convByName = conversions.associateBy { it.name.lowercase() }

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

    private fun isPiece(p: Product) = when (p.unit) {
        Unit.G, Unit.KG, Unit.L, Unit.ML -> false
        else -> true
    }

    private fun unitLabel(p: Product) = when (p.unit) {
        Unit.G -> "г"
        Unit.KG -> "кг"
        Unit.L -> "л"
        Unit.ML -> "мл"
        else -> "шт."
    }

    private fun productOf(row: Row): Product? = products.firstOrNull { it.id == row.productId }

    // Список мер для строки: своя единица + конверсии (для штучных — только с привязкой)
    private fun measureOptionsFor(row: Row): List<MeasureOption> {
        val product = productOf(row) ?: return listOf(MeasureOption("—", ""))
        val options = mutableListOf(MeasureOption(unitLabel(product), ""))
        val piece = isPiece(product)
        conversions.forEach { conv ->
            if (!piece || refsByPair.containsKey(product.id to conv.id)) {
                options.add(MeasureOption(conv.name, conv.name))
            }
        }
        if (piece && options.size == 1) {
            options.add(MeasureOption("Добавить привязку в настройках", "", enabled = false))
        }
        return options
    }

    // Стоимость позиции: null — если посчитать нельзя
    private fun costFor(row: Row): Double? {
        val product = productOf(row) ?: return null
        val amount = row.amountText.replace(',', '.').toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        val ing = Ingredient(product = product, amount = amount, measure = row.measure)
        if (row.measure.isNotEmpty()) {
            val conv = convByName[row.measure.lowercase()] ?: return null
            val perMeasure = refsByPair[row.productId to conv.id]
            ing.grams = when {
                perMeasure != null -> amount * perMeasure          // персональная привязка — приоритет
                !isPiece(product) -> amount * conv.grams          // весовые: общая конвертация
                else -> null                                      // штучные без привязки — не считаем
            }
        }
        return ing.cost
    }

    private fun costLabelFor(row: Row): String {
        if (row.productId <= 0) return ""
        if (row.amountText.replace(',', '.').toDoubleOrNull() ?: 0.0 <= 0) return ""
        val cost = costFor(row)
        return if (cost != null) "%.2f ₽".format(cost) else "нужна привязка"
    }

    private fun updateTotal() {
        var total = 0.0
        for (row in rows) {
            row.costLabel = costLabelFor(row)
            total += costFor(row) ?: 0.0
        }
        totalText.text = "Итого: %.2f ₽".format(total)
        if (rows.isNotEmpty()) adapter?.notifyItemRangeChanged(0, rows.size)
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val productInput: AutoCompleteTextView = v.findViewById(R.id.productInput)
            val amountInput: EditText = v.findViewById(R.id.amountInput)
            val measureInput: AutoCompleteTextView = v.findViewById(R.id.measureInput)
            val costText: TextView = v.findViewById(R.id.costText)
            val deleteBtn: ImageView = v.findViewById(R.id.deleteRowBtn)

            val productAdapter = ProductAdapter()
            val measureAdapter = MeasureAdapter()

            var row: Row? = null

            init {
                productInput.setAdapter(productAdapter)
                productInput.threshold = 1
                // пустое поле не открывает список само — открываем на тапе
                productInput.setOnClickListener { productInput.showDropDown() }

                productInput.setOnItemClickListener { _, _, pos, _ ->
                    val product = productAdapter.shown.getOrNull(pos) ?: return@setOnItemClickListener
                    if (row?.productId == product.id) return@setOnItemClickListener
                    row?.productId = product.id
                    // если текущая мера недопустима для нового продукта — сбрасываем на свою единицу
                    if (measureOptionsFor(row!!).none { it.value == row!!.measure && it.enabled }) {
                        row?.measure = ""
                    }
                    bindMeasure(this, row!!)
                    updateTotal()
                }

                measureInput.setAdapter(measureAdapter)
                measureInput.threshold = 1
                measureInput.setOnClickListener { measureInput.showDropDown() }

                measureInput.setOnItemClickListener { _, _, pos, _ ->
                    val option = measureAdapter.options.getOrNull(pos) ?: return@setOnItemClickListener
                    if (!option.enabled) {
                        Toast.makeText(this@AddRecipeActivity,
                            "Привязки настраиваются в разделе «Настройки»",
                            Toast.LENGTH_SHORT).show()
                        measureInput.setText(labelForMeasure(row!!), false)
                        return@setOnItemClickListener
                    }
                    row?.measure = option.value
                    updateTotal()
                }

                amountInput.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val t = s?.toString() ?: ""
                        if (row?.amountText == t) return  // setText из биндинга — не пересчитываем
                        row?.amountText = t
                        updateTotal()
                    }
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                })

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

        private fun labelForMeasure(row: Row): String {
            val product = productOf(row)
            return if (row.measure.isEmpty())
                product?.let { unitLabel(it) } ?: "—"
            else row.measure
        }

        private fun bindMeasure(holder: VH, row: Row) {
            holder.measureAdapter.setOptions(measureOptionsFor(row))
            holder.measureInput.setText(labelForMeasure(row), false)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ingredient_edit, parent, false)
        )

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]
            holder.row = row

            // --- Продукт ---
            holder.productAdapter.setData(products)
            holder.productInput.setText(productOf(row)?.name ?: "", false)

            // --- Мера ---
            bindMeasure(holder, row)

            // --- Количество и стоимость ---
            holder.amountInput.setText(row.amountText)
            holder.costText.text = row.costLabel
        }
    }

    // Адаптер продуктов с поиском: регистронезависимо, по подстроке в любом месте имени
    private inner class ProductAdapter :
        ArrayAdapter<String>(this@AddRecipeActivity, android.R.layout.simple_list_item_1) {

        var all: List<Product> = emptyList()
        var shown: List<Product> = emptyList()

        fun setData(list: List<Product>) {
            all = list
            shown = list
            notifyDataSetChanged()
        }

        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position].name

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            (v as TextView).text = shown[position].name
            return v
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
            getView(position, convertView, parent)

        override fun getFilter(): Filter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val q = (constraint?.toString() ?: "").trim().lowercase()
                val list = if (q.isEmpty()) all else all.filter { it.name.lowercase().contains(q) }
                return FilterResults().apply { values = list; count = list.size }
            }
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                shown = (results?.values as? List<Product>) ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    // Адаптер мер: список под конкретную строку, без фильтрации
    private inner class MeasureAdapter :
        ArrayAdapter<MeasureOption>(this@AddRecipeActivity, android.R.layout.simple_list_item_1) {

        var options: List<MeasureOption> = emptyList()

        fun setOptions(list: List<MeasureOption>) {
            options = list
            notifyDataSetChanged()
        }

        override fun getCount() = options.size
        override fun getItem(position: Int) = options[position].label

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            val tv = v as TextView
            tv.text = options[position].label
            if (!options[position].enabled) tv.alpha = 0.4f
            else tv.alpha = 1f
            return v
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
            getView(position, convertView, parent)

        override fun getFilter(): Filter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?) =
                FilterResults().apply { values = options; count = options.size }
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }
}
