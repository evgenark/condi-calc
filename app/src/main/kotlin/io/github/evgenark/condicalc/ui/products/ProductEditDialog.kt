package io.github.evgenark.condicalc.ui.products

import android.os.Build
import android.app.Dialog
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Product
import io.github.evgenark.condicalc.data.Repo
import io.github.evgenark.condicalc.data.MeasureUnit
import kotlinx.coroutines.launch

class ProductEditDialog(private val activity: FragmentActivity) {

    var onSaved: (() -> Unit)? = null

    private var product: Product? = null
    private var productId: Long = -1L
    private val convRows = mutableListOf<Triple<Long, EditText, Double?>>()
    private var existingRefs: Map<Long, Double> = emptyMap()
    private var selectedUnitIndex = 0

    private lateinit var name: EditText
    private lateinit var amount: EditText
    private lateinit var price: EditText
    private lateinit var convTitle: View
    private lateinit var convContainer: LinearLayout
    private lateinit var amountLayout: com.google.android.material.textfield.TextInputLayout

    fun show(id: Long) {
        productId = id
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_product_edit, null)
        dialog.setContentView(v)

        // Тап мимо полей сворачивает клавиатуру
        val hideKeyboard: () -> Unit = {
            val focused = dialog.currentFocus
            if (focused is EditText) {
                focused.clearFocus()
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(focused.windowToken, 0)
            }
        }
        v.setOnClickListener { hideKeyboard() }
        v.findViewById<View>(R.id.productContent).setOnClickListener { hideKeyboard() }

        // «Готово» в поле цены сворачивает клавиатуру
        v.findViewById<EditText>(R.id.editPackPrice).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { hideKeyboard(); true } else false
        }
        // Список продуктов обновится при любом закрытии диалога

        dialog.setOnDismissListener { onSaved?.invoke() }

        name = v.findViewById(R.id.editName)
        amount = v.findViewById(R.id.editPackAmount)
        price = v.findViewById(R.id.editPackPrice)
        convTitle = v.findViewById(R.id.convTitle)
        convContainer = v.findViewById(R.id.convContainer)
        amountLayout = v.findViewById(R.id.amountLayout)
        amountLayout.suffixText = MeasureUnit.entries[selectedUnitIndex].label
        amountLayout.setEndIconOnClickListener {
            val popup = androidx.appcompat.widget.ListPopupWindow(activity)
            popup.setAdapter(
                ArrayAdapter(activity, android.R.layout.simple_list_item_1,
                    MeasureUnit.entries.map { it.label })
            )
            popup.anchorView = amountLayout
            popup.setSelection(selectedUnitIndex)
            popup.setOnItemClickListener { _, _, pos, _ ->
                selectedUnitIndex = pos
                amountLayout.suffixText = MeasureUnit.entries[pos].label
                popup.dismiss()
            }
            popup.show()
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
                selectedUnitIndex = loaded.unit.ordinal
                amountLayout.suffixText = loaded.unit.label
                amount.setText(formatNum(loaded.packAmount))
                price.setText(formatNum(loaded.packPrice))
                convTitle.visibility = View.VISIBLE
                buildConvRows(loaded)
            }
        }

        v.findViewById<Button>(R.id.saveBtn).setOnClickListener { save(dialog) }

        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            setBackgroundDrawableResource(android.R.color.transparent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
                decorView.setOnApplyWindowInsetsListener { view, insets ->
                    val bars = insets.getInsets(
                        WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                    )
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                    insets
                }
            }
        }

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
            val input = com.google.android.material.textfield.TextInputEditText(activity).apply {
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
        val selectedUnit = MeasureUnit.entries[selectedUnitIndex]

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
