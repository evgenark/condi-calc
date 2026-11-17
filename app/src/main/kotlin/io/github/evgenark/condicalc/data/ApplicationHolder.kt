
package io.github.evgenark.condicalc.data

import android.app.Application

class CondiCalcApp : Application() {
    companion object {
        lateinit var context: Application
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}
