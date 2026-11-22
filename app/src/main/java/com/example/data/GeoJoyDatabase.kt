package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedLocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GeoJoyDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var instance: GeoJoyDatabase? = null

        fun getInstance(context: Context): GeoJoyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GeoJoyDatabase::class.java,
                    "geojoy_locations.db"
                ).build().also { instance = it }
            }
        }
    }
}
