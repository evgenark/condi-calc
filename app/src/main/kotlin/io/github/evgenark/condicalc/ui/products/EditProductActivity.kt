package io.github.evgenark.condicalc.ui.products

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Repo
import io.github.evgenark.condicalc.data.Unit

class EditProductActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        val index = intent.getIntExtra(EXTRA_INDEX, -1)
        if (index < 0 || index >= Repo.products.size) { finish(); return }
        val product = Repo.products[index]

        val name = findViewById<EditText>(R.id.editName)
        val unit = findViewById<Spinner>(R.id.editUnit)
        val amount = findViewById<EditText>(R.id.editPackAmount)
        val price = findViewById<EditText>(R.id.editPackPrice)

        name.setText(product.name)

        val units = Unit.entries.map { it.label }
        unit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)
        unit.setSelection(Unit.entries.indexOf(product.unit))

        amount.setText(product.packAmount.toString())
        price.setText(product.packPrice.toString())

        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            product.name = name.text.toString().ifBlank { product.name }
            product.unit = Unit.entries[unit.selectedItemPosition]
            product.packAmount = amount.text.toString().toDoubleOrNull() ?: product.packAmount
            product.packPrice = price.text.toString().toDoubleOrNull() ?: product.packPrice
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_INDEX = "index"
    }
}

