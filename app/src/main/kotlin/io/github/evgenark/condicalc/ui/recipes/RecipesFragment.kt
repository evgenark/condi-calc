package io.github.evgenark.condicalc.ui.recipes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Repo
import kotlinx.coroutines.launch

class RecipesFragment : Fragment(R.layout.fragment_recipes) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.recipeList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = RecipeAdapter(mutableListOf())
    }

    override fun onStart() {
        super.onStart()
        view?.findViewById<RecyclerView>(R.id.recipeList)?.let { list ->
            lifecycleScope.launch {
                val recipes = Repo.getAllRecipes()
                (list.adapter as? RecipeAdapter)?.let { adapter ->
                    adapter.recipes.clear()
                    adapter.recipes.addAll(recipes)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}
