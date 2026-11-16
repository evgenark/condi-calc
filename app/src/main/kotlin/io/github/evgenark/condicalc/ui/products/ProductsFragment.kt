package io.github.evgenark.condicalc.ui.products

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Repo

class ProductsFragment : Fragment(R.layout.fragment_products) {

    private var adapter: ProductAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.productList)
        adapter = ProductAdapter(Repo.products) { index ->
            val intent = Intent(requireContext(), EditProductActivity::class.java)
            intent.putExtra(EditProductActivity.EXTRA_INDEX, index)
            startActivity(intent)
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter?.notifyDataSetChanged()   // обновить цены после редактирования
    }
}

