package io.github.evgenark.condicalc.data

enum class Unit(val label: String, val isMass: Boolean) {
    KG("кг", true),
    G("г", true),
    L("л", false),
    ML("мл", false),
    PCS("шт", false),
    TSP("ч. л.", false),
    TBSP("ст. л.", false),
    CUP("стакан", false);
}

data class Product(
    var name: String,
    var unit: Unit = Unit.G,
    var packAmount: Double = 1.0,   // сколько в упаковке (в единицах unit)
    var packPrice: Double = 0.0,    // цена упаковки, ₽
    val id: Long = 0L
) {
    val pricePerUnit: Double
        get() = if (packAmount > 0) packPrice / packAmount else 0.0
}

data class Ingredient(
    val product: Product,
    var amount: Double,             // количество в единицах продукта
) {
    val cost get() = amount * product.pricePerUnit
}

data class Recipe(
    val name: String,
    val ingredients: MutableList<Ingredient>,
) {
    val totalCost get() = ingredients.sumOf { it.cost }
}

