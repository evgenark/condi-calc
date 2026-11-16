package io.github.evgenark.condicalc.data

object Repo {
    val products = mutableListOf(
        Product("Мука", Unit.KG, 2.0, 180.0),
        Product("Сахар", Unit.KG, 1.0, 110.0),
        Product("Масло сливочное", Unit.G, 180.0, 150.0),
        Product("Сгущёнка", Unit.G, 380.0, 95.0),
        Product("Яйца", Unit.PCS, 10.0, 90.0),
        Product("Молоко", Unit.L, 1.0, 85.0),
    )

    val recipes = mutableListOf(
        Recipe("Наполеон", mutableListOf(
            Ingredient(products[0], 0.4),
            Ingredient(products[2], 200.0),
            Ingredient(products[3], 380.0),
        )),
        Recipe("Медовик", mutableListOf(
            Ingredient(products[0], 0.45),
            Ingredient(products[1], 0.2),
            Ingredient(products[4], 3.0),
            Ingredient(products[2], 150.0),
        )),
    )
}

