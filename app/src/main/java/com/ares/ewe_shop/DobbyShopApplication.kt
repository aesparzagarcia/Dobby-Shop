package com.ares.ewe_shop

import android.app.Application
import com.ares.ewe_shop.session.ProactiveShopAccessTokenRefresh
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DobbyShopApplication : Application() {

    @Inject
    lateinit var proactiveShopAccessTokenRefresh: ProactiveShopAccessTokenRefresh

    override fun onCreate() {
        super.onCreate()
        proactiveShopAccessTokenRefresh.start()
    }
}
