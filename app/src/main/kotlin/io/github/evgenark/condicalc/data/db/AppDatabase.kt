
package io.github.evgenark.condicalc.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        RecipeEntity::class,
        RecipeItemEntity::class,
        UnitConversionEntity::class,
        UnitConversionRefEntity::class
    ],
    version = 1,
    exportSchema = false // schema-файлы не нужны, если миграции не планируешь сразу
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun recipeDao(): RecipeDao
    abstract fun itemDao(): RecipeItemDao
    abstract fun conversionDao(): UnitConversionDao
    abstract fun refDao(): UnitConversionRefDao
}
