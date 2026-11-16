package io.github.evgenark.condicalc.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Product

class ProductAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.productName)
        val price: TextView = v.findViewById(R.id.productPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
    )

    override fun getItemCount() = products.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val product = products[position]
        holder.name.text = product.name
        holder.price.text = "%.0f ₽/кг".format(product.pricePerKg)
    }
}

