package io.github.evgenark.condicalc.ui.recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import java.text.DecimalFormat
import java.util.Locale

class IngredientAdapter(
    private val onClick: (Int) -> Unit,
    private val onDelete: (Int) -> Unit,
) : RecyclerView.Adapter<IngredientAdapter.VH>() {

    val items = mutableListOf<RecipeIngredient>()

    fun submit(list: List<RecipeIngredient>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.ingredientName)
        val details: TextView = v.findViewById(R.id.ingredientDetails)
        val cost: TextView = v.findViewById(R.id.ingredientCost)
        val delete: View = v.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ing = items[position]
        holder.name.text = ing.product?.name ?: "?"
        holder.details.text = formatAmount(ing.amount) + " " + ing.measure.shortName
        holder.cost.text = "%.2f ₽".format(ing.cost)
        holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
        holder.delete.setOnClickListener { onDelete(holder.bindingAdapterPosition) }
    }

    private fun formatAmount(amount: Double): String {
        val df = if (amount % 1.0 == 0.0)
            DecimalFormat("0", DecimalFormatSymbols(Locale.getDefault()))
        else
            DecimalFormat("0.###", DecimalFormatSymbols(Locale.getDefault()))
        return df.format(amount)
    }
}
