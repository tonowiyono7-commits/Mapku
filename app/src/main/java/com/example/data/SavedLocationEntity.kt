package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object LocationCategory {
    const val FAVORITE = "FAVORITE"
    const val HISTORY = "HISTORY"
}

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)
