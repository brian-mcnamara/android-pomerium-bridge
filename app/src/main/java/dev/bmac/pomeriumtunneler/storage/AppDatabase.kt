package dev.bmac.pomeriumtunneler.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ RouteItem::class ], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun routesDao(): RoutesDao

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(ctx: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(ctx, AppDatabase::class.java, "routes_db")
                    .build().also { instance = it }
            }
        }

    }

}