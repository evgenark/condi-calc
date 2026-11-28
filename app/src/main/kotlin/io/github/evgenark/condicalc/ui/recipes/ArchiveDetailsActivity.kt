package io.github.evgenark.condicalc.ui.recipes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.evgenark.condicalc.R
import io.github.evgenark.condicalc.data.Repo
import io.github.evgenark.condicalc.data.db.ArchivedItemEntity
import io.github.evgenark.condicalc.data.db.ArchivedRecipeEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveDetailsActivity : AppCompatActivity() {

    private var archive: ArchivedRecipeEntity? = null
    private val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive_details)
        val archiveId = intent.getLongExtra("archive_id", 0L)

        lifecycleScope.launch {
            val a = Repo.getArchiveById(archiveId) ?: run { finish(); return@launch }
            archive = a
            title = a.name
            findViewById<TextView>(R.id.archiveDates).text =
                "Изделие создано: ${df.format(Date(a.createdAt))}\nВ архиве: ${df.format(Date(a.archivedAt))}"
            findViewById<TextView>(R.id.archiveTotal).text = "Итого: %.2f ₽".format(a.totalCost)
            val commentView = findViewById<TextView>(R.id.archiveComment)
            if (a.comment.isEmpty()) commentView.visibility = View.GONE
            else commentView.text = a.comment

            val container = findViewById<LinearLayout>(R.id.archiveItems)
            Repo.getArchiveItems(archiveId).forEach { item ->
                container.addView(TextView(this@ArchiveDetailsActivity).apply {
                    text = lineText(item)
                    textSize = 16f
                    setPadding(0, 12, 0, 0)
                })
            }
        }

        findViewById<View>(R.id.restoreBtn).setOnClickListener { confirmRestore() }
        findViewById<View>(R.id.deleteBtn).setOnClickListener { confirmDelete() }
    }

    private fun lineText(item: ArchivedItemEntity): String {
        val amountStr = if (item.amount % 1.0 == 0.0) item.amount.toLong().toString()
        else "%.1f".format(item.amount)
        val measureStr = item.measure.ifEmpty { item.unitLabel }
        val unitPart = if (measureStr.isNotEmpty()) " $measureStr" else ""
        val gramsUnit = when (item.unitLabel) {
            "кг", "г" -> "г"
            "л", "мл" -> "мл"
            else -> null
        }
        val gramsPart = item.grams?.let { g -> gramsUnit?.let { u -> " (≈ ${fmtNum(g)} $u)" } } ?: ""
        return "%s — %s%s%s — %.2f ₽".format(item.productName, amountStr, unitPart, gramsPart, item.cost)
    }

    private fun fmtNum(v: Double): String {
        val s = "%.2f".format(v)
        return s.removeSuffix(".00").removeSuffix(",00")
    }

    private fun confirmRestore() {
        val a = archive ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Восстановить изделие?")
            .setMessage("Будет создано новое изделие «${a.name}». Продукты ищутся по именам; для ненайденных появятся строки-заглушки с предупреждением.")
            .setPositiveButton("Восстановить") { _, _ ->
                lifecycleScope.launch {
                    val (newId, missing) = Repo.restoreRecipe(a.id)
                    if (newId == 0L) {
                        Toast.makeText(this@ArchiveDetailsActivity, "Не удалось восстановить", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val msg = if (missing.isEmpty()) "Изделие восстановлено"
                        else "Восстановлено. Не найдены: ${missing.joinToString(", ") { it.name }}"
                    Toast.makeText(this@ArchiveDetailsActivity, msg, Toast.LENGTH_LONG).show()
                    val intent = Intent(this@ArchiveDetailsActivity, AddRecipeActivity::class.java)
                        .putExtra("recipe_id", newId)
                    if (missing.isNotEmpty()) {
                        intent.putExtra("missing_names", missing.map { it.name }.toTypedArray())
                        intent.putExtra("missing_amounts", missing.map { it.amount }.toDoubleArray())
                        intent.putExtra("missing_measures", missing.map { it.measure }.toTypedArray())
                    }
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmDelete() {
        val a = archive ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Удалить снимок?")
            .setMessage("Снимок «${a.name}» от ${df.format(Date(a.archivedAt))} будет удалён безвозвратно. Живое изделие не пострадает.")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    Repo.deleteArchive(a.id)
                    finish()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
