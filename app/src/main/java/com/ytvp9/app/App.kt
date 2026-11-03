package com.ytvp9.app

import android.app.Application
import androidx.room.Room

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private lateinit var instance: App
        private var db: AppDatabase? = null

        fun db(context: android.content.Context): AppDatabase {
            return db ?: synchronized(this) {
                db ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "ytvp9.db"
                ).build().also { db = it }
            }
        }
    }
}
