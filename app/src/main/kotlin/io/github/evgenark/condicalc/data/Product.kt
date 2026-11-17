
package io.github.evgenark.condicalc.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String? = null,      // «ОС», «1С», «2С» для яиц, null для обычных
    val pricePerUnit: Double,
    val unit: String = "шт",           // базовая единица: шт / кг / г / л / мл
    val weightInGrams: Double? = null, // вес одной единицы в граммах (для шт/упаковок)
    val createdAt: Long = System.currentTimeMillis()
)
