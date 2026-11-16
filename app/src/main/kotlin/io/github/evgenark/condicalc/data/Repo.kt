package io.github.evgenark.condicalc.data

object Repo {
    val products = mutableListOf(
        Product("Мука", 90.0),
        Product("Сахар", 110.0),
        Product("Масло сливочное", 850.0),
        Product("Сгущёнка", 180.0),
        Product("Яйца", 350.0),
    )

    val recipes = mutableListOf(
        Recipe("Наполеон", mutableListOf(
            Ingredient(products[0], 400),
            Ingredient(products[2], 200),
            Ingredient(products[3], 380),
        )),
        Recipe("Медовик", mutableListOf(
            Ingredient(products[0], 450),
            Ingredient(products[1], 200),
            Ingredient(products[4], 100),
            Ingredient(products[2], 150),
        )),
    )
}

