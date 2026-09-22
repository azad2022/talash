package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import com.example.data.database.AppDatabase
import com.example.data.repository.ShopRepository

class GildarApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: ShopRepository by lazy { ShopRepository(database.shopDao, this) }

    override fun onCreate() {
        super.onCreate()
    }
}
