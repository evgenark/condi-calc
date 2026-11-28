package io.github.evgenark.condicalc.ui.products

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Product
import io.github.evgenark.condicalc.data.Repo
import kotlinx.coroutines.launch

class ProductsFragment : Fragment(R.layout.fragment_products) {

    private var adapter: ProductAdapter? = null
    private val products: MutableList<Product> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.productList) 
        adapter = ProductAdapter(
            products,
            onEdit = { id ->
                ProductEditDialog(requireActivity()).apply {
                    onSaved = { loadProducts() }
                }.show(id)
            },
            onDelete = { p -> confirmDelete(p) }
        )
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        view.findViewById<View>(R.id.addProductBtn).setOnClickListener {
            ProductEditDialog(requireActivity()).apply {
                onSaved = { loadProducts() }
            }.show(-1L)
        }
    }

    private fun confirmDelete(p: Product) {
        lifecycleScope.launch {
            val used = Repo.isProductUsed(p.id)
            if (used) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удалить «${p.name}»?")
                    .setMessage("Этот продукт участвует в рецептах. Сначала уберите его из рецептов — после этого продукт можно будет удалить.")
                    .setPositiveButton("Понятно", null)
                    .show()
            } else {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удалить «${p.name}»?")
                    .setMessage("Привязки мер этого продукта тоже удалятся.")
                    .setPositiveButton("Удалить") { _, _ ->
                        lifecycleScope.launch {
                            Repo.deleteProduct(p.id)
                            loadProducts()
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loadProducts()
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            val fresh = try {
                Repo.getAllProducts()
            } catch (e: Exception) {
                emptyList<Product>()
            }
            products.clear()
            products.addAll(fresh)
            adapter?.notifyDataSetChanged()
        }
    }
}
