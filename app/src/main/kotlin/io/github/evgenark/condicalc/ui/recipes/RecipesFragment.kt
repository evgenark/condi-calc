package io.github.evgenark.condicalc.ui.recipes

import android.content.Intent
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

    private val adapter = RecipeAdapter(mutableListOf())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.recipeList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        adapter.onClick = { recipe ->
            val intent = Intent(requireContext(), AddRecipeActivity::class.java)
            intent.putExtra("recipe_id", recipe.id)
            startActivity(intent)
        }

        view.findViewById<View>(R.id.addRecipeBtn).setOnClickListener {
            startActivity(Intent(requireContext(), AddRecipeActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val recipes = Repo.getAllRecipes()
            adapter.recipes.clear()
            adapter.recipes.addAll(recipes)
            adapter.notifyDataSetChanged()
        }
    }
}
