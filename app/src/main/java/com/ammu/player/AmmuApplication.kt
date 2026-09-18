package com.ammu.player

import android.app.Application
import com.ammu.player.data.db.AmmuDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AmmuApplication : Application() {

    lateinit var database: AmmuDatabase
        private set

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AmmuDatabase.getInstance(this, applicationScope)
    }
}
