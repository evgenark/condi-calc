
package io.github.evgenark.condicalc.data

import android.content.Context
import io.github.evgenark.condicalc.data.db.*
import io.github.evgenark.condicalc.data.Models.Unit
import kotlinx.coroutines.runBlocking

object Repo {
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        
        val db = DatabaseProvider.get(context)
        
        // Если база пустая — засеваем дефолтные продукты
        runBlocking {
            val products = db.productDao().getAll()
            if (products.isEmpty()) {
                val defaults = listOf(
                    ProductEntity(name = "Мука", unit = "KG", packAmount = 2.0, packPrice = 180.0),
                    ProductEntity(name = "Сахар", unit = "KG", packAmount = 1.0, packPrice = 110.0),
                    ProductEntity(name = "Масло сливочное", unit = "G", packAmount = 180.0, packPrice = 150.0),
                    ProductEntity(name = "Сгущёнка", unit = "G", packAmount = 380.0, packPrice = 95.0),
                    ProductEntity(name = "Яйца", unit = "PCS", packAmount = 10.0, packPrice = 90.0),
                    ProductEntity(name = "Молоко", unit = "L", packAmount = 1.0, packPrice = 85.0),
                )
                defaults.forEach { db.productDao().insert(it) }
            }
        }

        isInitialized = true
    }

    // --- Продукты ---

    suspend fun getAllProducts(): List<Product> {
        val db = DatabaseProvider.get(ApplicationHolder.context) // см. ниже про ApplicationHolder
        val entities = db.productDao().getAll()
        return entities.map { entityToModel(it) }
    }

    suspend fun getProductById(id: Long): Product? {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.productDao().getById(id)?.let { entityToModel(it) }
    }

    suspend fun saveProduct(product: Product): Long {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val entity = modelToEntity(product)
        return if (product.name.isNotEmpty()) { // простая валидация
            db.productDao().insert(entity)
        } else {
            throw IllegalArgumentException("Имя продукта не может быть пустым")
        }
    }

    suspend fun deleteProduct(id: Long) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.productDao().deleteById(id) // нужен маленький хелпер, см. ниже
    }

    // --- Рецепты ---

    suspend fun createRecipe(name: String): Long {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val recipe = RecipeEntity(name = name)
        return db.recipeDao().insert(recipe)
    }

    suspend fun addIngredientToRecipe(recipeId: Long, product: Product, amount: Double) {
        val db = DatabaseProvider.get(ApplicationHelper.context)
        val item = RecipeItemEntity(
            recipeId = recipeId,
            productId = product.id, // важно: у Product должен быть id, см. Models.kt
            amount = amount
        )
        db.itemDao().insert(item)
    }

    suspend fun getRecipeIngredients(recipeId: Long): List<Ingredient> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val items = db.itemDao().getByRecipe(recipeId)
        val productsMap = items.mapNotNull { it.productId }.let { ids ->
            db.productDao().getAll().associateBy { it.id } // грубая оптимизация: лучше отдельный запрос по id
        }
        return items.map { item ->
            productsMap[item.productId]?.let { entity ->
                Ingredient(product = entityToModel(entity), amount = item.amount)
            } ?: run { throw IllegalStateException("Продукт для ингредиента не найден") }
        }
    }

    // --- Конвертации (заготовка) ---
    suspend fun getAllConversions(): List<UnitConversionEntity> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.conversionDao().getAll()
    }

    // --- Хелперы конвертации ---

    private fun entityToModel(e: ProductEntity): Product {
        val unit = Unit.valueOf(e.unit) // "KG" -> Unit.KG
        return Product(
            name = e.name,
            unit = unit,
            packAmount = e.packAmount,
            packPrice = e.packPrice,
            id = e.id
        )
    }

    private fun modelToEntity(p: Product): ProductEntity {
        return ProductEntity(
            id = p.id,
            name = p.name,
            unit = p.unit.name, // Unit.KG -> "KG"
            packAmount = p.packAmount,
            packPrice = p.packPrice
        )
    }
}
