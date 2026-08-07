package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import com.example.data.database.AppDatabase
import com.example.data.repository.ShopRepository

class GildarApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: ShopRepository by lazy { ShopRepository(database.shopDao, this) }

    override fun attachBaseContext(base: Context?) {
        if (base != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            super.attachBaseContext(base.createAttributionContext("default"))
        } else {
            super.attachBaseContext(base)
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
