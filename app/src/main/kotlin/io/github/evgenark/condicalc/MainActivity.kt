package io.github.evgenark.condicalc

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val title = findViewById<TextView>(R.id.title)
        findViewById<Button>(R.id.btn).setOnClickListener {
            title.text = "Торт готов! 🍰"
        }
    }
}

