package com.ben.inly

import android.app.Application
import android.content.SharedPreferences
import com.ben.inly.data.local.room.AppDatabase
import com.ben.inly.di.androidModule
import com.ben.inly.di.sharedModule
import com.ben.inly.domain.ai.LocalAiEngine
import com.ben.inly.domain.selfhost.SelfHostSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class InlyApplication : Application() {
    companion object {
        @Volatile
        var isReady = false
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@InlyApplication)
            workManagerFactory()
            modules(sharedModule, androidModule)
        }

        getKoin().get<SelfHostSyncScheduler>()

        CoroutineScope(Dispatchers.IO).launch {
            getKoin().get<AppDatabase>()
            getKoin().get<SharedPreferences>()
            isReady = true
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                getKoin().get<LocalAiEngine>().warmUpGenerator()
            } catch (e: Exception) {
                // Silent — if pre-warm fails, first query just pays the load cost
            }
        }
    }
}