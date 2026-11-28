package io.github.evgenark.condicalc.ui.recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.db.ArchivedRecipeEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveAdapter : RecyclerView.Adapter<ArchiveAdapter.VH>() {

    val items = mutableListOf<ArchivedRecipeEntity>()
    var onClick: ((ArchivedRecipeEntity) -> Unit)? = null
    var onLongClick: ((ArchivedRecipeEntity) -> Unit)? = null

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.archiveName)
        val total: TextView = v.findViewById(R.id.archiveTotal)
        val date: TextView = v.findViewById(R.id.archiveDate)
    }

    private val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_archive_card, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        holder.name.text = a.name
        holder.total.text = "Итого: %.2f ₽".format(a.totalCost)
        holder.date.text = if (a.archivedAt > 0) df.format(Date(a.archivedAt)) else ""
        holder.itemView.setOnClickListener { onClick?.invoke(a) }
        holder.itemView.setOnLongClickListener { onLongClick?.invoke(a); true }
    }
}
