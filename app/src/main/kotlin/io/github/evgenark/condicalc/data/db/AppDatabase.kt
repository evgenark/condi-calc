package io.github.evgenark.condicalc.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        RecipeEntity::class,
        RecipeItemEntity::class,
        UnitConversionEntity::class,
        UnitConversionRefEntity::class,
        ArchivedRecipeEntity::class,
        ArchivedItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun recipeDao(): RecipeDao
    abstract fun itemDao(): RecipeItemDao
    abstract fun conversionDao(): UnitConversionDao
    abstract fun refDao(): UnitConversionRefDao
    abstract fun archiveDao(): ArchiveDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recipe_items ADD COLUMN measure TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recipes ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_items_recipeId ON recipe_items(recipeId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_items_productId ON recipe_items(productId)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS archived_recipes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "comment TEXT NOT NULL DEFAULT '', " +
                "createdAt INTEGER NOT NULL, " +
                "archivedAt INTEGER NOT NULL, " +
                "totalCost REAL NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS archived_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "archiveId INTEGER NOT NULL, " +
                "productName TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "measure TEXT NOT NULL DEFAULT '', " +
                "unitLabel TEXT NOT NULL DEFAULT '', " +
                "grams REAL, " +
                "cost REAL NOT NULL, " +
                "FOREIGN KEY(archiveId) REFERENCES archived_recipes(id) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_archived_items_archiveId ON archived_items(archiveId)")
    }
}
