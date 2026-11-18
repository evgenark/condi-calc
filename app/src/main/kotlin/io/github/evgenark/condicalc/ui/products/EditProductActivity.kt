package io.github.evgenark.condicalc.ui.products

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
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

    private var product: Product? = null
    private val convRows = mutableListOf<Triple<Long, EditText, Double?>>() // id меры, поле ввода, вес по умолчанию

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) { finish(); return }

        val name = findViewById<EditText>(R.id.editName)
        val unit = findViewById<Spinner>(R.id.editUnit)
        val amount = findViewById<EditText>(R.id.editPackAmount)
        val price = findViewById<EditText>(R.id.editPackPrice)
        val convContainer = findViewById<LinearLayout>(R.id.convContainer)

        lifecycleScope.launch {
            val loaded = Repo.getProductById(id) ?: run {
                finish()
                return@launch
            }
            product = loaded

            name.setText(loaded.name)
            val units = Unit.entries.map { it.label }
            unit.adapter = ArrayAdapter(this@EditProductActivity, android.R.layout.simple_spinner_dropdown_item, units)
            unit.setSelection(Unit.entries.indexOf(loaded.unit))
            amount.setText(loaded.packAmount.toString())
            price.setText(loaded.packPrice.toString())

            // Строим строки мер
            val conversions = Repo.getAllConversions()
            val refs = Repo.getRefsForProduct(id).associate { it.conversionId to it.quantity }
            convContainer.removeAllViews()
            convRows.clear()
            conversions.forEach { conv ->
                val row = LinearLayout(this@EditProductActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val label = TextView(this@EditProductActivity).apply {
                    text = conv.name
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val input = EditText(this@EditProductActivity).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    hint = "по умолчанию ${conv.grams} г"
                    refs[conv.id]?.let { setText(it.toString()) }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { width = 400 }
                }
                row.addView(label)
                row.addView(input)
                convContainer.addView(row)
                convRows.add(Triple(conv.id, input, conv.grams))
            }
        }

        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val current = product ?: return@setOnClickListener
            current.name = name.text.toString().ifBlank { current.name }
            current.unit = Unit.entries[unit.selectedItemPosition]
            current.packAmount = amount.text.toString().toDoubleOrNull() ?: current.packAmount
            current.packPrice = price.text.toString().toDoubleOrNull() ?: current.packPrice

            val values = convRows.mapNotNull { (convId, input, _) ->
                input.text.toString().toDoubleOrNull()?.let { convId to it }
            }.toMap()

            lifecycleScope.launch {
                Repo.updateProduct(current)
                Repo.saveProductRefs(id, values)
                Toast.makeText(this@EditProductActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
