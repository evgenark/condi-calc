package io.github.evgenark.condicalc.ui.recipes

import android.os.Bundle
import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Filter
import android.widget.Filter.FilterResults
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Ingredient
import io.github.evgenark.condicalc.data.Product
import io.github.evgenark.condicalc.data.Repo
import io.github.evgenark.condicalc.data.Unit
import kotlinx.coroutines.launch

class AddRecipeActivity : AppCompatActivity() {

    private class Row {
        var productId: Long = -1L
        var amountText: String = ""
        var measure: String = ""   // "" — в единицах продукта
        var costLabel: String = ""
    }

    private data class MeasureOption(val label: String, val value: String, val enabled: Boolean = true)

    private val products = mutableListOf<Product>()
    private val conversions = mutableListOf<io.github.evgenark.condicalc.data.db.UnitConversionEntity>()
    private val rows = mutableListOf<Row>()
    private var adapter: IngredientAdapter? = null
    private var convByName: Map<String, io.github.evgenark.condicalc.data.db.UnitConversionEntity> = emptyMap()
    private var refsByPair: Map<Pair<Long, Long>, Double> = emptyMap()
    private lateinit var totalText: TextView
    private lateinit var emptyHint: TextView
    private var editRecipeId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)
        totalText = findViewById(R.id.totalText)
        emptyHint = findViewById(R.id.emptyHint)

        editRecipeId = intent.getLongExtra("recipe_id", 0L)
        if (editRecipeId > 0) title = "Редактирование рецепта"

        val list = findViewById<RecyclerView>(R.id.ingredientEditList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = IngredientAdapter()
        list.adapter = adapter

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.addIngredientBtn).setOnClickListener {
            rows.add(Row())
            adapter?.notifyItemInserted(rows.size - 1)
            openEditSheet(rows.size - 1)
        }
        findViewById<android.widget.ImageButton>(R.id.menuBtn).setOnClickListener { anchor ->
            showEditMenu(anchor)
        }

        lifecycleScope.launch { loadDictionaries() }
    }

    private fun showEditMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_recipe_edit, popup.menu)
        // У нового рецепта удалять нечего
        if (editRecipeId == 0L) popup.menu.removeItem(R.id.action_delete_recipe)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> { save(); true }
                R.id.action_save_as -> { saveAsCopy(); true }
                R.id.action_delete_recipe -> { confirmDelete(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun collectItems(): List<Triple<Long, Double, String>> = rows.mapNotNull { row ->
        val amount = row.amountText.replace(',', '.').toDoubleOrNull()
        if (row.productId > 0 && amount != null && amount > 0)
            Triple(row.productId, amount, row.measure)
        else null
    }

    private fun saveAsCopy() {
        val name = findViewById<EditText>(R.id.recipeNameInput).text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название рецепта", Toast.LENGTH_SHORT).show()
            return
        }
        val items = collectItems()
        if (items.isEmpty()) {
            Toast.makeText(this, "Заполните хотя бы одну строку: продукт и количество", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            Repo.saveRecipe("$name (копия)", items, 0)
            Toast.makeText(this@AddRecipeActivity, "Копия создана", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmDelete() {
        val name = findViewById<EditText>(R.id.recipeNameInput).text.toString().trim()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Удалить рецепт?")
            .setMessage("«$name» будет удалён безвозвратно")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    Repo.deleteRecipe(editRecipeId)
                    finish()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP) {
            val focused = currentFocus
            if (focused is EditText) {
                val rect = Rect()
                focused.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    focused.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(focused.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
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
                    val own = when (ing.product.unit) {
                        Unit.G -> "грамм"; Unit.KG -> "килограмм"
                        Unit.L -> "литр"; Unit.ML -> "миллилитр"
                        else -> "штук."
                    }
                    if (measure.equals(own, ignoreCase = true)) measure = ""
                    amountText = if (ing.amount % 1.0 == 0.0)
                        ing.amount.toInt().toString()
                    else
                        "%.1f".format(ing.amount)
                })
            }
        }

        refresh()
    }

    private fun refresh() {
        var total = 0.0
        for (row in rows) {
            row.costLabel = costLabelFor(row)
            total += costFor(row) ?: 0.0
        }
        totalText.text = "Итого: %.2f ₽".format(total)
        emptyHint.isVisible = rows.isEmpty()
        adapter?.notifyDataSetChanged()
    }

    private var View.isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(value) { visibility = if (value) View.VISIBLE else View.GONE }

    private fun deleteRow(position: Int) {
        if (position == RecyclerView.NO_POSITION || position >= rows.size) return
        rows.removeAt(position)
        adapter?.notifyItemRemoved(position)
        refresh()
    }

    private fun openEditSheet(index: Int) {
        val row = rows.getOrNull(index) ?: return
        val sheet = BottomSheetDialog(this)
        val v = layoutInflater.inflate(R.layout.sheet_ingredient_edit, null)
        sheet.setContentView(v)

        val productInput = v.findViewById<AutoCompleteTextView>(R.id.sheetProductInput)
        val amountInput = v.findViewById<EditText>(R.id.sheetAmountInput)
        val measureInput = v.findViewById<AutoCompleteTextView>(R.id.sheetMeasureInput)

        val productAdapter = ProductAdapter()
        productInput.setAdapter(productAdapter)
        productAdapter.setData(products)
        productInput.threshold = 1
        productInput.setOnClickListener { productInput.showDropDown() }

        val measureAdapter = MeasureAdapter()
        measureInput.setAdapter(measureAdapter)
        measureInput.threshold = 1
        measureInput.setOnClickListener { measureInput.showDropDown() }

        ViewCompat.setOnApplyWindowInsetsListener(v) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (imeVisible) {
                if (productInput.isPopupShowing) {
                    productInput.dismissDropDown()
                    productInput.post { productInput.showDropDown() }
                }
                if (measureInput.isPopupShowing) {
                    measureInput.dismissDropDown()
                    measureInput.post { measureInput.showDropDown() }
                }
            }
            insets
        }

        productInput.setText(productOf(row)?.name ?: "", false)

        fun bindMeasure() {
            measureAdapter.submitOptions(measureOptionsFor(row))
            measureInput.setText(labelForMeasure(row), false)
        }
        bindMeasure()
        amountInput.setText(row.amountText)

        productInput.setOnItemClickListener { _, _, pos, _ ->
            val product = productAdapter.shown.getOrNull(pos) ?: return@setOnItemClickListener
            if (row.productId == product.id) return@setOnItemClickListener
            row.productId = product.id
            if (measureOptionsFor(row).none { it.value == row.measure && it.enabled }) {
                row.measure = ""
            }
            bindMeasure()
            adapter?.notifyItemChanged(index)
        }

        measureInput.setOnItemClickListener { _, _, pos, _ ->
            val option = measureAdapter.options.getOrNull(pos) ?: return@setOnItemClickListener
            if (!option.enabled) {
                Toast.makeText(this, "Привязки настраиваются в разделе «Настройки»", Toast.LENGTH_SHORT).show()
                measureInput.setText(labelForMeasure(row), false)
                return@setOnItemClickListener
            }
            row.measure = option.value
            adapter?.notifyItemChanged(index)
        }

        v.findViewById<MaterialButton>(R.id.sheetSaveBtn).setOnClickListener {
            val amount = amountInput.text.toString().replace(',', '.').toDoubleOrNull()
            when {
                row.productId <= 0 ->
                    Toast.makeText(this, "Выберите продукт", Toast.LENGTH_SHORT).show()
                amount == null || amount <= 0 ->
                    Toast.makeText(this, "Введите количество", Toast.LENGTH_SHORT).show()
                else -> {
                    row.amountText = amountInput.text.toString().trim()
                    sheet.dismiss()
                    refresh()
                }
            }
        }

        sheet.show()
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
        
        val items = collectItems()
        
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
        else -> "шт"
    }

    private fun productOf(row: Row): Product? = products.firstOrNull { it.id == row.productId }

    private fun measureOptionsFor(row: Row): List<MeasureOption> {
        val product = productOf(row) ?: return listOf(MeasureOption("—", ""))
        val options = mutableListOf(MeasureOption(unitLabel(product), ""))
        val piece = isPiece(product)
        // Собственная единица уже стоит первой в списке — конвертацию с тем же
        // смыслом (грамм для «г», килограмм для «кг» и т.д.) дублировать не нужно
        val ownName = when (product.unit) {
            Unit.G -> "грамм"
            Unit.KG -> "килограмм"
            Unit.L -> "литр"
            Unit.ML -> "миллилитр"
            else -> "штук."
        }
        conversions.forEach { conv ->
            if (conv.name.equals(ownName, ignoreCase = true)) return@forEach
            if (!piece || refsByPair.containsKey(product.id to conv.id)) {
                options.add(MeasureOption(conv.name, conv.name))
            }
        }
        if (piece && options.size == 1) {
            options.add(MeasureOption("Добавить привязку в настройках", "", enabled = false))
        }
        return options
    }

    private fun costFor(row: Row): Double? {
        val product = productOf(row) ?: return null
        val amount = row.amountText.replace(',', '.').toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        val ing = Ingredient(product = product, amount = amount, measure = row.measure)
        if (row.measure.isNotEmpty()) {
            val conv = convByName[row.measure.lowercase()] ?: return null
            val perMeasure = refsByPair[row.productId to conv.id]
            ing.grams = when {
                perMeasure != null -> amount * perMeasure
                !isPiece(product) -> amount * conv.grams
                else -> null
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

    private fun labelForMeasure(row: Row): String {
        val product = productOf(row)
        return if (row.measure.isEmpty())
            product?.let { unitLabel(it) } ?: "—"
        else row.measure
    }

    private inner class IngredientAdapter : RecyclerView.Adapter<IngredientAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.ingredientName)
            val details: TextView = v.findViewById(R.id.ingredientDetails)
            val cost: TextView = v.findViewById(R.id.ingredientCost)
            val delete: View = v.findViewById(R.id.deleteButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ingredient, parent, false)
        )

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]
            val product = productOf(row)

            holder.name.text = product?.name ?: "Выберите продукт"
            val amount = row.amountText.replace(',', '.').toDoubleOrNull()
            val amountStr = when {
                amount == null -> "—"
                amount % 1.0 == 0.0 -> amount.toLong().toString()
                else -> amount.toString()
            }
            holder.details.text = "$amountStr ${labelForMeasure(row)}"
            holder.cost.text = row.costLabel

            holder.itemView.setOnClickListener { openEditSheet(holder.bindingAdapterPosition) }
            holder.delete.setOnClickListener { deleteRow(holder.bindingAdapterPosition) }
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
        ArrayAdapter<String>(this@AddRecipeActivity, android.R.layout.simple_list_item_1) {

        var options: List<MeasureOption> = emptyList()

        fun submitOptions(list: List<MeasureOption>) {
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
            tv.alpha = if (options[position].enabled) 1f else 0.4f
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
