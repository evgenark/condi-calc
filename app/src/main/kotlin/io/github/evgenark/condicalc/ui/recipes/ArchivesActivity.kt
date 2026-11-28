package io.github.evgenark.condicalc.ui.recipes

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Repo
import io.github.evgenark.condicalc.data.db.ArchivedRecipeEntity
import kotlinx.coroutines.launch

class ArchivesActivity : AppCompatActivity() {

    private val adapter = ArchiveAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archives)
        title = "Архив"

        findViewById<RecyclerView>(R.id.archiveList).apply {
            layoutManager = LinearLayoutManager(this@ArchivesActivity)
            adapter = this@ArchivesActivity.adapter
        }
        adapter.onClick = { a ->
            startActivity(Intent(this, ArchiveDetailsActivity::class.java).putExtra("archive_id", a.id))
        }
        adapter.onLongClick = { a -> confirmDelete(a) }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            adapter.items.clear()
            adapter.items.addAll(Repo.getAllArchives())
            adapter.notifyDataSetChanged()
            findViewById<View>(R.id.emptyHint).visibility =
                if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun confirmDelete(a: ArchivedRecipeEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Удалить снимок?")
            .setMessage("«${a.name}» будет удалён из архива безвозвратно. Живое изделие не пострадает.")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    Repo.deleteArchive(a.id)
                    load()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
