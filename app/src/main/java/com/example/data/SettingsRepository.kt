package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "geojoy_settings")

enum class SpeedMode(val labelId: String, val defaultKmh: Float) {
    WALK("walk", 5.0f),
    CYCLE("cycle", 18.0f),
    DRIVE("drive", 55.0f)
}

enum class MapLayerStyle {
    DEFAULT,
    SATELLITE,
    TERRAIN,
    DARK
}

data class GeoJoySettings(
    val walkSpeedKmh: Float = 5.0f,
    val cycleSpeedKmh: Float = 18.0f,
    val driveSpeedKmh: Float = 55.0f,
    val selectedSpeedMode: SpeedMode = SpeedMode.WALK,
    val mapLayerStyle: MapLayerStyle = MapLayerStyle.DEFAULT,
    val updateIntervalMs: Long = 250L,
    val enableGpsJitter: Boolean = true,
    val keepMapCentered: Boolean = true,
    val joystickOnLeft: Boolean = false,
    val showTrail: Boolean = true
) {
    val activeSpeedKmh: Float
        get() = when (selectedSpeedMode) {
            SpeedMode.WALK -> walkSpeedKmh
            SpeedMode.CYCLE -> cycleSpeedKmh
            SpeedMode.DRIVE -> driveSpeedKmh
        }

    val activeSpeedMetersPerSec: Double
        get() = activeSpeedKmh / 3.6
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val WALK_SPEED = floatPreferencesKey("walk_speed_kmh")
        val CYCLE_SPEED = floatPreferencesKey("cycle_speed_kmh")
        val DRIVE_SPEED = floatPreferencesKey("drive_speed_kmh")
        val SPEED_MODE = stringPreferencesKey("speed_mode")
        val MAP_STYLE = stringPreferencesKey("map_style")
        val UPDATE_INTERVAL = longPreferencesKey("update_interval_ms")
        val GPS_JITTER = booleanPreferencesKey("enable_gps_jitter")
        val KEEP_CENTERED = booleanPreferencesKey("keep_map_centered")
        val JOYSTICK_LEFT = booleanPreferencesKey("joystick_on_left")
        val SHOW_TRAIL = booleanPreferencesKey("show_trail")
    }

    val settingsFlow: Flow<GeoJoySettings> = context.dataStore.data.map { prefs ->
        GeoJoySettings(
            walkSpeedKmh = prefs[Keys.WALK_SPEED] ?: 5.0f,
            cycleSpeedKmh = prefs[Keys.CYCLE_SPEED] ?: 18.0f,
            driveSpeedKmh = prefs[Keys.DRIVE_SPEED] ?: 55.0f,
            selectedSpeedMode = prefs[Keys.SPEED_MODE]?.let {
                runCatching { SpeedMode.valueOf(it) }.getOrNull()
            } ?: SpeedMode.WALK,
            mapLayerStyle = prefs[Keys.MAP_STYLE]?.let {
                runCatching { MapLayerStyle.valueOf(it) }.getOrNull()
            } ?: MapLayerStyle.DEFAULT,
            updateIntervalMs = prefs[Keys.UPDATE_INTERVAL] ?: 250L,
            enableGpsJitter = prefs[Keys.GPS_JITTER] ?: true,
            keepMapCentered = prefs[Keys.KEEP_CENTERED] ?: true,
            joystickOnLeft = prefs[Keys.JOYSTICK_LEFT] ?: false,
            showTrail = prefs[Keys.SHOW_TRAIL] ?: true
        )
    }

    suspend fun setSpeedMode(mode: SpeedMode) {
        context.dataStore.edit { it[Keys.SPEED_MODE] = mode.name }
    }

    suspend fun setWalkSpeed(kmh: Float) {
        context.dataStore.edit { it[Keys.WALK_SPEED] = kmh.coerceIn(1f, 15f) }
    }

    suspend fun setCycleSpeed(kmh: Float) {
        context.dataStore.edit { it[Keys.CYCLE_SPEED] = kmh.coerceIn(10f, 45f) }
    }

    suspend fun setDriveSpeed(kmh: Float) {
        context.dataStore.edit { it[Keys.DRIVE_SPEED] = kmh.coerceIn(25f, 140f) }
    }

    suspend fun setMapLayerStyle(style: MapLayerStyle) {
        context.dataStore.edit { it[Keys.MAP_STYLE] = style.name }
    }

    suspend fun setUpdateInterval(ms: Long) {
        context.dataStore.edit { it[Keys.UPDATE_INTERVAL] = ms }
    }

    suspend fun setEnableGpsJitter(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GPS_JITTER] = enabled }
    }

    suspend fun setKeepMapCentered(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_CENTERED] = enabled }
    }

    suspend fun setJoystickOnLeft(onLeft: Boolean) {
        context.dataStore.edit { it[Keys.JOYSTICK_LEFT] = onLeft }
    }

    suspend fun setShowTrail(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TRAIL] = enabled }
    }
}
