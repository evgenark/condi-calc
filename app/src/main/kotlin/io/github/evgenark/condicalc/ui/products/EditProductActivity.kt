package io.github.evgenark.condicalc.ui.products

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) { finish(); return }

        val name = findViewById<EditText>(R.id.editName)
        val unit = findViewById<Spinner>(R.id.editUnit)
        val amount = findViewById<EditText>(R.id.editPackAmount)
        val price = findViewById<EditText>(R.id.editPackPrice)

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
        }

        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val current = product ?: return@setOnClickListener
            current.name = name.text.toString().ifBlank { current.name }
            current.unit = Unit.entries[unit.selectedItemPosition]
            current.packAmount = amount.text.toString().toDoubleOrNull() ?: current.packAmount
            current.packPrice = price.text.toString().toDoubleOrNull() ?: current.packPrice

            lifecycleScope.launch {
                Repo.updateProduct(current)
                Toast.makeText(this@EditProductActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
