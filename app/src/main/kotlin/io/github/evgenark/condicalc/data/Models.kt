package io.github.evgenark.condicalc.data

data class Product(
    val name: String,
    var pricePerKg: Double,      // руб. за кг
)

data class Ingredient(
    val product: Product,
    var amountG: Int,            // граммы в рецепте
) {
    val cost get() = amountG / 1000.0 * product.pricePerKg
}

data class Recipe(
    val name: String,
    val ingredients: MutableList<Ingredient>,
) {
    val totalCost get() = ingredients.sumOf { it.cost }
}

