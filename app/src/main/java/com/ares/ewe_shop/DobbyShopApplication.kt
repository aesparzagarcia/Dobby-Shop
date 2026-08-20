package com.ares.ewe_shop

import android.app.Application
import com.ares.ewe_shop.core.crash.CrashlyticsJourney
import com.ares.ewe_shop.data.local.datastore.SessionManager
import com.ares.ewe_shop.session.ProactiveShopAccessTokenRefresh
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class DobbyShopApplication : Application() {

    @Inject
    lateinit var proactiveShopAccessTokenRefresh: ProactiveShopAccessTokenRefresh

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        CrashlyticsJourney.setApp("dobby_shop")
        runBlocking { sessionManager.prepareSession() }
        proactiveShopAccessTokenRefresh.start()
        MapsInitializerFacade.initializeLatestRenderer(applicationContext)
    }
}
