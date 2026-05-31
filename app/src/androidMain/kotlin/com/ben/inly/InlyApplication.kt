package com.ben.inly

import android.app.Application
import android.content.SharedPreferences
import com.ben.inly.data.local.room.AppDatabase
import com.ben.inly.di.androidModule
import com.ben.inly.di.sharedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class InlyApplication : Application() {
    companion object {
        @Volatile
        var isReady = false
    }

    override fun onCreate() {
        super.onCreate()
        startKoin { androidContext(this@InlyApplication); modules(sharedModule, androidModule) }

        CoroutineScope(Dispatchers.IO).launch {
            getKoin().get<AppDatabase>()
            getKoin().get<SharedPreferences>() // EncryptedSharedPreferences is also slow
            isReady = true
        }
    }
}