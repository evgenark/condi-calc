package io.github.evgenark.condicalc.ui.recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Ingredient

class IngredientAdapter(private val items: MutableList<Ingredient>) :
    RecyclerView.Adapter<IngredientAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.ingName)
        val amount: TextView = v.findViewById(R.id.ingAmount)
        val cost: TextView = v.findViewById(R.id.ingCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient_row, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.product.name

        val unitLabel = if (item.measure.isNotEmpty()) item.measure else item.product.unit.label
        holder.amount.text = "%.1f %s".format(item.amount, unitLabel)

        // Для жидкостей grams — это миллилитры, для массовых — граммы
        val gLabel = if (item.product.unit.isVolume) "мл" else "г"

        val cost = item.cost
        holder.cost.text = when {
            item.grams != null && cost > 0 -> "%.0f %s · %.2f ₽".format(item.grams, gLabel, cost)
            item.grams != null -> "%.0f %s".format(item.grams, gLabel)
            cost > 0 -> "%.2f ₽".format(cost)          // штучные в единицах продукта
            else -> "—"                                 // штучные с мерой: цена не считается
        }
    }

    fun update(newItems: List<Ingredient>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
