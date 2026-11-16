package io.github.evgenark.condicalc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.evgenark.condicalc.ui.products.ProductsFragment
import io.github.evgenark.condicalc.ui.recipes.RecipesFragment
import io.github.evgenark.condicalc.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            replace(RecipesFragment())
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recipes  -> replace(RecipesFragment())
                R.id.nav_products -> replace(ProductsFragment())
                R.id.nav_settings -> replace(SettingsFragment())
            }
            true
        }
    }

    private fun replace(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}

