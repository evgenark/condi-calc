package io.github.evgenark.condicalc.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Repo
import io.github.evgenark.condicalc.data.db.UnitConversionEntity
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val conversions: MutableList<UnitConversionEntity> = mutableListOf()
    private var adapter: ConvAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ConvAdapter(
            onEdit = { item -> showEditDialog(item) },
            onDelete = { item -> confirmDelete(item) }
        )
        view.findViewById<RecyclerView>(R.id.convList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SettingsFragment.adapter
        }
        view.findViewById<View>(R.id.addConvBtn).setOnClickListener {
            showEditDialog(null)
        }
    }

    override fun onStart() {
        super.onStart()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            conversions.clear()
            conversions.addAll(Repo.getAllConversions())
            adapter?.notifyDataSetChanged()
        }
    }

    private fun showEditDialog(existing: UnitConversionEntity?) {
        val context = context ?: return
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val nameInput = EditText(context).apply {
            hint = "Название (стакан, ст. л. ...)"
            setText(existing?.name ?: "")
        }
        val gramsInput = EditText(context).apply {
            hint = "Вес, грамм"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(existing?.grams?.toString() ?: "")
        }
        container.addView(nameInput)
        container.addView(gramsInput)

        AlertDialog.Builder(context)
            .setTitle(if (existing == null) "Новая мера" else "Редактировать")
            .setView(container)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = nameInput.text.toString().trim()
                val grams = gramsInput.text.toString().toDoubleOrNull()
                if (name.isEmpty() || grams == null || grams <= 0) return@setPositiveButton
                lifecycleScope.launch {
                    if (existing == null) {
                        Repo.saveConversion(name, grams)
                    } else {
                        Repo.updateConversion(existing.copy(name = name, grams = grams))
                    }
                    load()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmDelete(item: UnitConversionEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить «${item.name}»?")
            .setMessage("Привязки продуктов к этой мере тоже удалятся.")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    Repo.deleteConversion(item)
                    load()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private inner class ConvAdapter(
        val onEdit: (UnitConversionEntity) -> Unit,
        val onDelete: (UnitConversionEntity) -> Unit,
    ) : RecyclerView.Adapter<ConvAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: android.widget.TextView = v.findViewById(R.id.convName)
            val grams: android.widget.TextView = v.findViewById(R.id.convGrams)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val v = layoutInflater.inflate(R.layout.item_conversion, parent, false)
            return VH(v)
        }

        override fun getItemCount() = conversions.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = conversions[position]
            holder.name.text = item.name
            holder.grams.text = "%.1f г".format(item.grams)
            holder.itemView.setOnClickListener { onEdit(item) }
            holder.itemView.setOnLongClickListener {
                onDelete(item)
                true
            }
        }
    }
}
