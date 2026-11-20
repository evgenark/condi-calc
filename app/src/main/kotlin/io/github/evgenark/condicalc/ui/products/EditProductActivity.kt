package io.github.evgenark.condicalc.ui.products

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Product
import io.github.evgenark.condicalc.data.Repo
import io.github.evgenark.condicalc.data.Unit
import kotlinx.coroutines.launch

class EditProductActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "product_id"
    }

    private var product: Product? = null
    private var productId: Long = -1L
    private val convRows = mutableListOf<Triple<Long, EditText, Double?>>() // id меры, поле, дефолт в граммах
    private var existingRefs: Map<Long, Double> = emptyMap()

    private lateinit var name: EditText
    private lateinit var unit: Spinner
    private lateinit var amount: EditText
    private lateinit var price: EditText
    private lateinit var convTitle: View
    private lateinit var convContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        name = findViewById(R.id.editName)
        unit = findViewById(R.id.editUnit)
        amount = findViewById(R.id.editPackAmount)
        price = findViewById(R.id.editPackPrice)
        convTitle = findViewById(R.id.convTitle)
        convContainer = findViewById(R.id.convContainer)

        unit.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Unit.entries.map { it.label }
        )

        productId = intent.getLongExtra(EXTRA_ID, -1L)

        if (productId < 0) {
            // Новый продукт: блок мер появим после первого сохранения
            convTitle.visibility = View.GONE
            convContainer.visibility = View.GONE
        } else {
            lifecycleScope.launch {
                val loaded = Repo.getProductById(productId) ?: run { finish(); return@launch }
                product = loaded
                fillForm(loaded)
            }
        }

        findViewById<Button>(R.id.saveBtn).setOnClickListener { save() }
    }

    private fun fillForm(p: Product) {
        name.setText(p.name)
        unit.setSelection(p.unit.ordinal)
        amount.setText(formatNum(p.packAmount))
        price.setText(formatNum(p.packPrice))
        convTitle.visibility = View.VISIBLE

        lifecycleScope.launch { buildConvRows(p) }
    }

    private suspend fun buildConvRows(p: Product) {
        convContainer.removeAllViews()
        convRows.clear()
        val convs = Repo.getAllConversions()
        existingRefs = Repo.getRefsForProduct(p.id).associate { it.conversionId to it.quantity }

        for (conv in convs) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val label = TextView(this).apply {
                text = conv.name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                gravity = Gravity.END
                hint = "по умолчанию ${formatNum(conv.grams)} г"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            existingRefs[conv.id]?.let { input.setText(formatNum(it)) }
            row.addView(label)
            row.addView(input)
            convContainer.addView(row)
            convRows.add(Triple(conv.id, input, conv.grams))
        }
    }

    private fun save() {
        val nameStr = name.text.toString().trim()
        if (nameStr.isEmpty()) { toast("Введите название"); return }
        val packAmount = amount.text.toString().replace(',', '.').toDoubleOrNull()
            ?: run { toast("Проверьте объём упаковки"); return }
        val packPrice = price.text.toString().replace(',', '.').toDoubleOrNull()
            ?: run { toast("Проверьте цену упаковки"); return }
        val selectedUnit = Unit.entries[unit.selectedItemPosition]

        lifecycleScope.launch {
            if (product == null) {
                // Создание
                val newId = Repo.saveProduct(
                    Product(name = nameStr, unit = selectedUnit, packAmount = packAmount, packPrice = packPrice)
                )
                productId = newId
                val loaded = Repo.getProductById(newId) ?: run { finish(); return@launch }
                product = loaded
                convTitle.visibility = View.VISIBLE
                convContainer.visibility = View.VISIBLE
                buildConvRows(loaded)
                toast("Продукт сохранён — теперь можно задать меры")
            } else {
                // Обновление
                product!!.name = nameStr
                product!!.unit = selectedUnit
                product!!.packAmount = packAmount
                product!!.packPrice = packPrice
                Repo.updateProduct(product!!)
                saveRefs()
                toast("Сохранено")
            }
        }
    }

    private suspend fun saveRefs() {
        val p = product ?: return
        val values = mutableMapOf<Long, Double>()
        for ((convId, input, _) in convRows) {
            val text = input.text.toString().replace(',', '.').trim()
            values[convId] = text.toDoubleOrNull() ?: existingRefs[convId] ?: continue
        }
        Repo.saveProductRefs(p.id, values)
    }

    private fun formatNum(v: Double): String {
        val s = "%.2f".format(v)
        return s.removeSuffix(",00").removeSuffix(".00")
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

