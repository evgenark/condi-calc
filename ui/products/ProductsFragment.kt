package io.github.evgenark.condicalc.ui.products

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Repo

class ProductsFragment : Fragment(R.layout.fragment_products) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.productList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = ProductAdapter(Repo.products)
    }
}

