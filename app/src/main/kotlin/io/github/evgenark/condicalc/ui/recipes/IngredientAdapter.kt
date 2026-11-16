package io.github.evgenark.condicalc.ui.recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Ingredient

class IngredientAdapter(private val items: List<Ingredient>) :
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
        holder.amount.text = "%.1f %s".format(item.amount, item.product.unit.label)
        holder.cost.text = "%.0f ₽".format(item.cost)
    }
}

