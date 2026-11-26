package io.github.evgenark.condicalc.ui.products

import android.app.Dialog
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Product
import io.github.evgenark.condicalc.data.Repo
import io.github.evgenark.condicalc.data.Unit as DataUnit
import kotlinx.coroutines.launch

class ProductEditDialog(private val activity: FragmentActivity) {

    var onSaved: (() -> Unit)? = null

    private var product: Product? = null
    private var productId: Long = -1L
    private val convRows = mutableListOf<Triple<Long, EditText, Double?>>()
    private var existingRefs: Map<Long, Double> = emptyMap()

    private lateinit var name: EditText
    private lateinit var unit: Spinner
    private lateinit var amount: EditText
    private lateinit var price: EditText
    private lateinit var convTitle: View
    private lateinit var convContainer: LinearLayout

    fun show(id: Long) {
        productId = id
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_product_edit, null)
        dialog.setContentView(v)
        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        // Список продуктов обновится при любом закрытии диалога
        dialog.setOnDismissListener { onSaved?.invoke() }

        name = v.findViewById(R.id.editName)
        unit = v.findViewById(R.id.editUnit)
        amount = v.findViewById(R.id.editPackAmount)
        price = v.findViewById(R.id.editPackPrice)
        convTitle = v.findViewById(R.id.convTitle)
        convContainer = v.findViewById(R.id.convContainer)

        unit.adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            Unit.entries.map { it.label }
        )

        // Тап мимо полей сворачивает клавиатуру
        v.setOnClickListener {
            val focused = dialog.currentFocus
            if (focused is EditText) {
                focused.clearFocus()
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(focused.windowToken, 0)
            }
        }

        if (productId < 0) {
            // Новый продукт: блок мер появим после первого сохранения
            convTitle.visibility = View.GONE
            convContainer.visibility = View.GONE
        } else {
            activity.lifecycleScope.launch {
                val loaded = Repo.getProductById(productId)
                    ?: run { dialog.dismiss(); return@launch }
                product = loaded
                name.setText(loaded.name)
                unit.setSelection(loaded.unit.ordinal)
                amount.setText(formatNum(loaded.packAmount))
                price.setText(formatNum(loaded.packPrice))
                convTitle.visibility = View.VISIBLE
                buildConvRows(loaded)
            }
        }

        v.findViewById<Button>(R.id.saveBtn).setOnClickListener { save(dialog) }

        dialog.show()
    }

    private suspend fun buildConvRows(p: Product) {
        convContainer.removeAllViews()
        convRows.clear()
        val convs = Repo.getAllConversions()
        existingRefs = Repo.getRefsForProduct(p.id).associate { it.conversionId to it.quantity }

        for (conv in convs) {
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val label = android.widget.TextView(activity).apply {
                text = conv.name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val input = EditText(activity).apply {
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

    private fun save(dialog: Dialog) {
        val nameStr = name.text.toString().trim()
        if (nameStr.isEmpty()) { toast("Введите название"); return }
        val packAmount = amount.text.toString().replace(',', '.').toDoubleOrNull()
            ?: run { toast("Проверьте объём упаковки"); return }
        val packPrice = price.text.toString().replace(',', '.').toDoubleOrNull()
            ?: run { toast("Проверьте цену упаковки"); return }
        val selectedUnit = DataUnit.entries[unit.selectedItemPosition]

        activity.lifecycleScope.launch {
            if (product == null) {
                // Создание: диалог не закрываем, чтобы задать меры
                val newId = Repo.saveProduct(
                    Product(name = nameStr, unit = selectedUnit, packAmount = packAmount, packPrice = packPrice)
                )
                productId = newId
                val loaded = Repo.getProductById(newId)
                    ?: run { dialog.dismiss(); return@launch }
                product = loaded
                convTitle.visibility = View.VISIBLE
                convContainer.visibility = View.VISIBLE
                buildConvRows(loaded)
                toast("Продукт сохранён — теперь можно задать меры")
            } else {
                // Обновление: сохраняем и закрываем
                product!!.name = nameStr
                product!!.unit = selectedUnit
                product!!.packAmount = packAmount
                product!!.packPrice = packPrice
                Repo.updateProduct(product!!)
                saveRefs()
                toast("Сохранено")
                dialog.dismiss()
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
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}
