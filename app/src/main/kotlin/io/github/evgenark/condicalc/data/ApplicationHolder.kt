package io.github.evgenark.condicalc.data

import android.app.Application

object ApplicationHolder {
    lateinit var context: Application
}

class CondiCalcApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationHolder.context = this
        Repo.init(this)
    }
}
