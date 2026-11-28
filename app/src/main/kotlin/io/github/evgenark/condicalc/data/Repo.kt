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
                    ProductEntity(name = "Яйцо", unit = "PCS", packAmount = 10.0, packPrice = 90.0),
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
        db.refDao().deleteByProduct(id)   // чистим привязки мер, иначе останутся сироты
        db.productDao().deleteById(id)
    }

    suspend fun isProductUsed(productId: Long): Boolean {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val recipes = db.recipeDao().getAll()
        return recipes.any { recipe ->
            db.itemDao().getByRecipe(recipe.id).any { it.productId == productId }
        }
    }

    suspend fun seedConversionsIfEmpty() {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        if (db.conversionDao().getAll().isNotEmpty()) return

        val defaults = listOf(
            "грамм" to 1.0,
            "килограмм" to 1000.0,
            "литр" to 1000.0,
            "миллилитр" to 1.0,
            "стакан" to 250.0,
            "столовая ложка" to 18.0,
            "чайная ложка" to 5.0,
            "штук." to 1.0
        )
        defaults.forEach { (name, grams) ->
            db.conversionDao().insert(UnitConversionEntity(name = name, grams = grams))
        }
    }

    // --- Рецепты ---

    suspend fun createRecipe(name: String): Long {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.recipeDao().insert(RecipeEntity(name = name, createdAt = System.currentTimeMillis()))
    }

    suspend fun getAllRecipes(): List<Recipe> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val recipeEntities = db.recipeDao().getAll()
        return recipeEntities.map { entity ->
            val items = getRecipeIngredients(entity.id)
            Recipe(id = entity.id, name = entity.name, createdAt = entity.createdAt, ingredients = items.toMutableList())
        }
    }

    suspend fun getRecipeById(id: Long): Recipe? {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val entity = db.recipeDao().getById(id) ?: return null
        return Recipe(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            ingredients = getRecipeIngredients(entity.id).toMutableList()
        )
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
                ing.grams = when (product.unit) {
                    MeasureUnit.G  -> item.amount
                    MeasureUnit.KG -> item.amount * 1000
                    else -> null
                }
            } else {
                convByName[item.measure.lowercase()]?.let { conv ->
                    val perMeasure = refsByPair[item.productId to conv.id] ?: conv.grams
                    ing.grams = when (product.unit) {
                        MeasureUnit.G, MeasureUnit.KG, MeasureUnit.L, MeasureUnit.ML -> item.amount * perMeasure
                        else -> null   // штучные и прочие — цена только в единицах продукта
                    }
                }
            }
            ing
        }
    }

    suspend fun saveRecipe(name: String, items: List<Triple<Long, Double, String>>, recipeId: Long = 0L): Long {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val id: Long = if (recipeId == 0L) {
            // новый рецепт
            db.recipeDao().insert(RecipeEntity(name = name, createdAt = System.currentTimeMillis()))
        } else {
            // редактирование: имя обновляем, дату создания сохраняем, ингредиенты перезаписываем
            val existing = db.recipeDao().getById(recipeId) ?: return 0L
            db.recipeDao().update(RecipeEntity(id = recipeId, name = name, createdAt = existing.createdAt))
            db.itemDao().deleteByRecipe(recipeId)
            recipeId
        }
        items.forEach { (productId, amount, measure) ->
            db.itemDao().insert(
                RecipeItemEntity(
                    recipeId = id,
                    productId = productId,
                    amount = amount,
                    measure = measure
                )
            )
        }
        return id
    }

    suspend fun deleteRecipe(recipeId: Long) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.itemDao().deleteByRecipe(recipeId)
        db.recipeDao().deleteById(recipeId)
    }
    
    // --- Архив ---

    suspend fun archiveRecipe(recipeId: Long, comment: String): Long? {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val recipe = db.recipeDao().getById(recipeId) ?: return null
        val ingredients = getRecipeIngredients(recipeId)

        val archiveId = db.archiveDao().insert(
            ArchivedRecipeEntity(
                name = recipe.name,
                comment = comment.trim(),
                createdAt = recipe.createdAt,
                archivedAt = System.currentTimeMillis(),
                totalCost = ingredients.sumOf { it.cost }
            )
        )
        db.archiveDao().insertAll(
            ingredients.map { ing ->
                ArchivedItemEntity(
                    archiveId = archiveId,
                    productName = ing.product.name,
                    amount = ing.amount,
                    measure = ing.measure,
                    unitLabel = ing.product.unit.label,
                    grams = ing.grams,
                    cost = ing.cost
                )
            }
        )
        return archiveId
    }

    suspend fun getAllArchives(): List<ArchivedRecipeEntity> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.archiveDao().getAll()
    }

    suspend fun getArchiveById(id: Long): ArchivedRecipeEntity? {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.archiveDao().getById(id)
    }

    suspend fun getArchiveItems(archiveId: Long): List<ArchivedItemEntity> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.archiveDao().getItems(archiveId)
    }

    suspend fun deleteArchive(id: Long) {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        db.archiveDao().deleteById(id)   // строки удалятся каскадом по внешнему ключу
    }

    // Восстановление снимка: новый рецепт, ингредиенты ищутся по именам.
    // Возвращает id нового рецепта и список ненайденных продуктов (для строк-заглушек в редакторе)
    suspend fun restoreRecipe(archiveId: Long): Pair<Long, List<MissingIngredient>> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        val archive = db.archiveDao().getById(archiveId) ?: return 0L to emptyList()
        val items = db.archiveDao().getItems(archiveId)
        val productsByName = db.productDao().getAll().associateBy { it.name.lowercase() }

        val restoredId = db.recipeDao().insert(
            RecipeEntity(name = archive.name, createdAt = System.currentTimeMillis())
        )
        val missing = mutableListOf<MissingIngredient>()
        for (item in items) {
            val product = productsByName[item.productName.lowercase()]
            if (product == null) {
                missing.add(MissingIngredient(item.productName, item.amount, item.measure))
                continue
            }
            db.itemDao().insert(
                RecipeItemEntity(
                    recipeId = restoredId,
                    productId = product.id,
                    amount = item.amount,
                    measure = item.measure
                )
            )
        }
        return restoredId to missing
    }

    suspend fun getAllRefs(): List<UnitConversionRefEntity> {
        val db = DatabaseProvider.get(ApplicationHolder.context)
        return db.refDao().getAll()
    }

    suspend fun getRecipeTotal(recipeId: Long): Double =
        getRecipeIngredients(recipeId).sumOf { it.cost }

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
            unit = MeasureUnit.valueOf(e.unit),
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
