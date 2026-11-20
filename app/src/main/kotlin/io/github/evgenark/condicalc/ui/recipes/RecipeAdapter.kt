package io.github.evgenark.condicalc.ui.recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Ingredient
import io.github.evgenark.condicalc.data.Recipe

class RecipeAdapter(val recipes: MutableList<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.recipeName)
        val cost: TextView = v.findViewById(R.id.recipeCost)
        val list: RecyclerView = v.findViewById(R.id.ingredientList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
    )

    override fun getItemCount() = recipes.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val recipe = recipes[position]
        holder.name.text = recipe.name
        holder.cost.text = "Итого: %.2f ₽".format(recipe.totalCost)
        holder.list.isNestedScrollingEnabled = false
        holder.list.layoutManager = LinearLayoutManager(holder.list.context)
        holder.list.adapter = IngredientAdapter(recipe.ingredients)
    }
}

