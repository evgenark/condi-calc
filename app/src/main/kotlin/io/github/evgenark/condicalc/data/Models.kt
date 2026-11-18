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
    var amount: Double,
    var measure: String = "",   // "" — в единицах продукта
) {
    var grams: Double? = null   // граммы заполняет репозиторий по конвертациям

    val cost: Double
        get() = when {
            measure.isEmpty() -> amount * product.pricePerUnit
            else -> {
                val g = grams ?: 0.0
                val perGram = when (product.unit) {
                    Unit.G  -> product.packPrice / product.packAmount
                    Unit.KG -> product.packPrice / (product.packAmount * 1000)
                    else -> 0.0  // шт/л/мл в граммы не переводим — цена через конвертацию не считается
                }
                g * perGram
            }
        }
}

data class Recipe(
    val name: String,
    val ingredients: MutableList<Ingredient>,
) {
    val totalCost get() = ingredients.sumOf { it.cost }
}

