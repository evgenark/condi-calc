package io.github.evgenark.condicalc.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,          // имя из enum Unit: "KG", "G", "L", "ML", "PCS", "TSP", "TBSP", "CUP"
    val packAmount: Double,    // сколько в упаковке (в единицах unit)
    val packPrice: Double,     // цена упаковки
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "recipe_items",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecipeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val productId: Long,
    val amount: Double,        // количество в единицах продукта (как в Ingredient)
    val measure: String = "",   // "" — в единицах продукта, иначе имя меры из настроек
)

// Справочник конвертаций: «стакан», «ст. л.» и т.п. — сколько это граммов
@Entity(tableName = "unit_conversions")
data class UnitConversionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val grams: Double,
)

// Привязка конвертации к конкретному продукту: «стакан белка = 10 шт яиц»
@Entity(
    tableName = "unit_conversion_refs",
    foreignKeys = [
        ForeignKey(
            entity = UnitConversionEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversionId"])]
)
data class UnitConversionRefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversionId: Long,
    val productId: Long,
    val quantity: Double,
)
