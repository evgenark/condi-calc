
package io.github.evgenark.condicalc.data

import android.content.Context
import androidx.room.Room
import io.github.evgenark.condicalc.data.db.AppDatabase
import io.github.evgenark.condicalc.data.db.MIGRATION_1_2
object DatabaseProvider {
    private var _db: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        if (_db == null) {
            _db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "condi_calc.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
        return _db!!
    }
}
