
package io.github.evgenark.condicalc.data

enum class Unit(val label: String, val isMass: Boolean) {
    KG("кг", true),
    G("г", true),
    L("л", false),
    ML("мл", false),
    PCS("шт", false),
    TSP("ч.л", false),
    TBSP("ст.л", false),
    CUP("стакан", false);

    val isVolume: Boolean get() = this == L || this == ML
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

    // Цена в «человеческой» единице: г → за кг, мл → за литр, остальное — как есть
    val pricePerStd: Double
        get() = when (unit) {
            Unit.G, Unit.ML -> pricePerUnit * 1000
            else -> pricePerUnit
        }

    val stdUnitLabel: String
        get() = when (unit) {
            Unit.G, Unit.KG -> "кг"
            Unit.ML, Unit.L -> "л"
            else -> unit.label   // шт, ч. л., ст. л., стакан
        }
}

data class Ingredient(
    val product: Product,
    var amount: Double,
    var measure: String = "",   // "" — в единицах продукта
) {
    // Граммы (массовые продукты) или миллилитры (жидкости); null — цена через меру не считается
    var grams: Double? = null

    val cost: Double
        get() {
            if (measure.isEmpty()) return amount * product.pricePerUnit
            val g = grams ?: return 0.0
            return when (product.unit) {
                Unit.G  -> g * product.pricePerUnit
                Unit.KG -> g * product.pricePerUnit / 1000.0
                Unit.L  -> g * product.pricePerUnit / 1000.0   // g = мл, жидкость как вода
                Unit.ML -> g * product.pricePerUnit
                else    -> 0.0   // PCS и прочие: цена только в единицах продукта
            }
        }
}

data class Recipe(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = 0,
    val ingredients: MutableList<Ingredient>,
) {
    val totalCost get() = ingredients.sumOf { it.cost }
}
