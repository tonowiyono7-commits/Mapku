package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM saved_locations WHERE category = :category ORDER BY createdAt DESC")
    fun getLocationsByCategory(category: String): Flow<List<SavedLocationEntity>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM saved_locations WHERE category = 'FAVORITE' " +
            "AND ABS(latitude - :lat) < 0.0002 AND ABS(longitude - :lng) < 0.0002)"
    )
    fun isFavorite(lat: Double, lng: Double): Flow<Boolean>

    @Query("SELECT * FROM saved_locations WHERE category = 'HISTORY' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestHistory(): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocationEntity)

    @Query("UPDATE saved_locations SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Query("UPDATE saved_locations SET createdAt = :timestamp, name = :name, address = :address WHERE id = :id")
    suspend fun touchHistoryEntry(id: Long, name: String, address: String, timestamp: Long)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Long)

    @Query(
        "DELETE FROM saved_locations WHERE category = 'FAVORITE' " +
            "AND ABS(latitude - :lat) < 0.0002 AND ABS(longitude - :lng) < 0.0002"
    )
    suspend fun deleteFavoriteNear(lat: Double, lng: Double)

    @Query("DELETE FROM saved_locations WHERE category = 'HISTORY'")
    suspend fun clearHistory()
}
