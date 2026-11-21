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
        UnitConversionRefEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun recipeDao(): RecipeDao
    abstract fun itemDao(): RecipeItemDao
    abstract fun conversionDao(): UnitConversionDao
    abstract fun refDao(): UnitConversionRefDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recipe_items ADD COLUMN measure TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recipes ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
    }
}
