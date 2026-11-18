package io.github.evgenark.condicalc.data

import android.content.Context
import io.github.evgenark.condicalc.data.db.*
import kotlinx.coroutines.runBlocking

object Repo {
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        val db = DatabaseProvider.get(context)

        runBlocking {
            if (db.productDao().getAll().isEmpty()) {
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
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.productDao().getAll().map { entityToModel(it) }
    }

    suspend fun getProductById(id: Long): Product? {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.productDao().getById(id)?.let { entityToModel(it) }
    }

    suspend fun saveProduct(product: Product): Long {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        if (product.name.isEmpty()) {
            throw IllegalArgumentException("Имя продукта не может быть пустым")
        }
        return db.productDao().insert(modelToEntity(product))
    }

    suspend fun updateProduct(product: Product) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.productDao().update(modelToEntity(product))
    }

    suspend fun deleteProduct(id: Long) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.productDao().deleteById(id)
    }

    // --- Рецепты ---

    suspend fun createRecipe(name: String): Long {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.recipeDao().insert(RecipeEntity(name = name))
    }

    suspend fun getAllRecipes(): List<Recipe> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val recipeEntities = db.recipeDao().getAll()
        return recipeEntities.map { entity ->
            val items = getRecipeIngredients(entity.id)
            Recipe(name = entity.name, ingredients = items.toMutableList())
        }
    }

    suspend fun addIngredientToRecipe(recipeId: Long, product: Product, amount: Double) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.itemDao().insert(
            RecipeItemEntity(
                recipeId = recipeId,
                productId = product.id,
                amount = amount
            )
        )
    }

    suspend fun getRecipeIngredients(recipeId: Long): List<Ingredient> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val items = db.itemDao().getByRecipe(recipeId)
        if (items.isEmpty()) return emptyList()

        val productsById = db.productDao().getAll().associateBy { it.id }
        val convByName = db.conversionDao().getAll().associateBy { it.name.lowercase() }
        val refsByPair = db.refDao().getAll()
            .associate { (it.productId to it.conversionId) to it.quantity }

        return items.mapNotNull { item ->
            val entity = productsById[item.productId] ?: return@mapNotNull null
            val product = entityToModel(entity)
            val ing = Ingredient(product = product, amount = item.amount, measure = item.measure)

            if (item.measure.isEmpty()) {
    // Количество в единицах продукта — граммы выводим, где можем
                ing.grams = when (product.unit) {
                    Unit.G  -> item.amount
                    Unit.KG -> item.amount * 1000
                    else -> null
                }
            } else {
    // Мера из настроек: персональный вес продукта или дефолт меры
                convByName[item.measure.lowercase()]?.let { conv ->
                    val perMeasure = refsByPair[item.productId to conv.id] ?: conv.grams
                    ing.grams = item.amount * perMeasure
                }
            }
            ing
        }
    }
    
    // --- Конвертации ---

    suspend fun getAllConversions(): List<UnitConversionEntity> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.conversionDao().getAll()
    }

    suspend fun saveConversion(name: String, grams: Double): Long {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.conversionDao().insert(UnitConversionEntity(name = name, grams = grams))
    }

    suspend fun updateConversion(conversion: UnitConversionEntity) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.conversionDao().update(conversion)
    }

    suspend fun deleteConversion(conversion: UnitConversionEntity) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.refDao().deleteByConversion(conversion.id) // чистим привязки продуктов
        db.conversionDao().delete(conversion)
    }

    suspend fun getRefsForProduct(productId: Long): List<UnitConversionRefEntity> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.refDao().getByProduct(productId)
    }

    suspend fun saveProductRefs(productId: Long, values: Map<Long, Double>) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.refDao().deleteByProduct(productId)
        if (values.isNotEmpty()) {
            db.refDao().insertAll(values.map { (convId, grams) ->
                UnitConversionRefEntity(conversionId = convId, productId = productId, quantity = grams)
            })
        }
    }

    // --- Хелперы конвертации ---

    private fun entityToModel(e: ProductEntity): Product {
        return Product(
            id = e.id,
            name = e.name,
            unit = Unit.valueOf(e.unit),
            packAmount = e.packAmount,
            packPrice = e.packPrice
        )
    }

    private fun modelToEntity(p: Product): ProductEntity {
        return ProductEntity(
            id = p.id,
            name = p.name,
            unit = p.unit.name,
            packAmount = p.packAmount,
            packPrice = p.packPrice
        )
    }
}
