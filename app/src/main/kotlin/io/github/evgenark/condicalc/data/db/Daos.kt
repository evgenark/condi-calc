package io.github.evgenark.condicalc.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface ProductDao {
    @Insert suspend fun insert(product: ProductEntity): Long
    @Update suspend fun update(product: ProductEntity)
    @Delete suspend fun delete(product: ProductEntity)

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RecipeDao {
    @Insert suspend fun insert(recipe: RecipeEntity): Long
    @Update suspend fun update(recipe: RecipeEntity)
    @Delete suspend fun delete(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes ORDER BY id ASC")
    suspend fun getAll(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): RecipeEntity?
}

@Dao
interface RecipeItemDao {
    @Insert suspend fun insert(item: RecipeItemEntity): Long
    @Insert suspend fun insertAll(items: List<RecipeItemEntity>)
    @Update suspend fun update(item: RecipeItemEntity)
    @Delete suspend fun delete(item: RecipeItemEntity)

    @Query("SELECT * FROM recipe_items WHERE recipeId = :recipeId ORDER BY id ASC")
    suspend fun getByRecipe(recipeId: Long): List<RecipeItemEntity>

    @Query("DELETE FROM recipe_items WHERE recipeId = :recipeId")
    suspend fun deleteByRecipe(recipeId: Long)
}

@Dao
interface UnitConversionDao {
    @Insert suspend fun insert(conversion: UnitConversionEntity): Long
    @Update suspend fun update(conversion: UnitConversionEntity)
    @Delete suspend fun delete(conversion: UnitConversionEntity)

    @Query("SELECT * FROM unit_conversions ORDER BY name ASC")
    suspend fun getAll(): List<UnitConversionEntity>

    @Query("SELECT * FROM unit_conversions WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): UnitConversionEntity?
}

@Dao
interface UnitConversionRefDao {
    @Insert suspend fun insertAll(refs: List<UnitConversionRefEntity>)

    @Query("SELECT * FROM unit_conversion_refs WHERE conversionId = :conversionId")
    suspend fun getByConversion(conversionId: Long): List<UnitConversionRefEntity>

    @Query("SELECT * FROM unit_conversion_refs WHERE productId = :productId")
    suspend fun getByProduct(productId: Long): List<UnitConversionRefEntity>

    @Query("DELETE FROM unit_conversion_refs WHERE conversionId = :conversionId")
    suspend fun deleteByConversion(conversionId: Long)

    @Query("DELETE FROM unit_conversion_refs WHERE productId = :productId")
    suspend fun deleteByProduct(productId: Long)

    @Query("SELECT * FROM unit_conversion_refs")
    suspend fun getAll(): List<UnitConversionRefEntity>

    @Delete suspend fun delete(ref: UnitConversionRefEntity)
}
