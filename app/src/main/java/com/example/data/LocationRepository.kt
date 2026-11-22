package com.example.data

import com.example.location.GeoMath
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val locationDao: LocationDao) {
    val favorites: Flow<List<SavedLocationEntity>> =
        locationDao.getLocationsByCategory(LocationCategory.FAVORITE)

    val history: Flow<List<SavedLocationEntity>> =
        locationDao.getLocationsByCategory(LocationCategory.HISTORY)

    fun isFavorite(lat: Double, lng: Double): Flow<Boolean> =
        locationDao.isFavorite(lat, lng)

    suspend fun saveFavorite(name: String, address: String, lat: Double, lng: Double) {
        locationDao.insertLocation(
            SavedLocationEntity(
                name = name.trim().ifBlank { "Titik Tersimpan" },
                address = address,
                latitude = lat,
                longitude = lng,
                category = LocationCategory.FAVORITE
            )
        )
    }

    suspend fun renameLocation(id: Long, newName: String) {
        val cleaned = newName.trim()
        if (cleaned.isNotEmpty()) {
            locationDao.updateName(id, cleaned)
        }
    }

    suspend fun removeFavoriteNear(lat: Double, lng: Double) {
        locationDao.deleteFavoriteNear(lat, lng)
    }

    suspend fun recordHistory(name: String, address: String, lat: Double, lng: Double) {
        val cleanName = name.trim().ifBlank { "Lokasi Kustom" }
        val latest = locationDao.getLatestHistory()
        if (latest != null && GeoMath.distanceMeters(latest.latitude, latest.longitude, lat, lng) < 30.0) {
            locationDao.touchHistoryEntry(latest.id, cleanName, address, System.currentTimeMillis())
        } else {
            locationDao.insertLocation(
                SavedLocationEntity(
                    name = cleanName,
                    address = address,
                    latitude = lat,
                    longitude = lng,
                    category = LocationCategory.HISTORY
                )
            )
        }
    }

    suspend fun deleteById(id: Long) {
        locationDao.deleteLocationById(id)
    }

    suspend fun clearHistory() {
        locationDao.clearHistory()
    }
}
