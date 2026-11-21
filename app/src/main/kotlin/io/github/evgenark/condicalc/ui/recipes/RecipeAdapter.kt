package io.github.evgenark.condicalc.ui.recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Recipe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecipeAdapter(val recipes: MutableList<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.VH>() {

    var onClick: ((Recipe) -> Unit)? = null

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.recipeName)
        val cost: TextView = v.findViewById(R.id.recipeCost)
        val date: TextView = v.findViewById(R.id.recipeDate)
    }

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
    )

    override fun getItemCount() = recipes.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val recipe = recipes[position]
        holder.name.text = recipe.name
        holder.cost.text = "Итого: %.2f ₽".format(recipe.totalCost)
        holder.date.text = if (recipe.createdAt > 0) dateFormat.format(Date(recipe.createdAt)) else ""
        holder.itemView.setOnClickListener { onClick?.invoke(recipe) }
    }
}
