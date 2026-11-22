package com.example.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.GeoJoySettings
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot

enum class MockRunStatus {
    IDLE,
    ACTIVE_SYSTEM,
    ACTIVE_PREVIEW
}

data class MockLocationState(
    val latitude: Double = -6.175392,
    val longitude: Double = 106.827153,
    val bearing: Float = 0f,
    val currentSpeedKmh: Float = 0f,
    val status: MockRunStatus = MockRunStatus.IDLE,
    val isDeveloperMockAuthorized: Boolean = false,
    val joystickDx: Float = 0f,
    val joystickDy: Float = 0f,
    val isCruiseLocked: Boolean = false,
    val trail: List<GeoPointData> = emptyList()
)

class MockLocationController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val engine = MockLocationEngine(context)

    private val _state = MutableStateFlow(
        MockLocationState(
            isDeveloperMockAuthorized = engine.isMockAppAuthorized()
        )
    )
    val state: StateFlow<MockLocationState> = _state.asStateFlow()

    @Volatile
    var latestSettings: GeoJoySettings = GeoJoySettings()
        private set

    @Volatile
    var isServiceLoopActive: Boolean = false

    private var fallbackLoopJob: Job? = null

    init {
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                latestSettings = settings
            }
        }
    }

    fun refreshMockAuthorization() {
        val authorized = engine.isMockAppAuthorized()
        _state.update { current ->
            val updatedStatus = when {
                current.status == MockRunStatus.IDLE -> MockRunStatus.IDLE
                authorized -> MockRunStatus.ACTIVE_SYSTEM
                else -> MockRunStatus.ACTIVE_PREVIEW
            }
            current.copy(
                isDeveloperMockAuthorized = authorized,
                status = updatedStatus
            )
        }
    }

    fun startSpoofing() {
        val authorized = engine.isMockAppAuthorized()
        val systemStarted = if (authorized) engine.startMockProviders() else false

        val hasLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        _state.update {
            it.copy(
                isDeveloperMockAuthorized = authorized,
                status = if (systemStarted || authorized) {
                    MockRunStatus.ACTIVE_SYSTEM
                } else {
                    MockRunStatus.ACTIVE_PREVIEW
                },
                trail = if (it.trail.isEmpty()) {
                    listOf(GeoPointData(it.latitude, it.longitude))
                } else {
                    it.trail
                }
            )
        }

        if (hasLocationPermission) {
            try {
                val intent = Intent(context, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                ensureFallbackLoopRunning()
            }
        } else {
            ensureFallbackLoopRunning()
        }
    }

    fun stopSpoofing() {
        fallbackLoopJob?.cancel()
        fallbackLoopJob = null
        engine.stopMockProviders()
        _state.update {
            it.copy(
                status = MockRunStatus.IDLE,
                joystickDx = 0f,
                joystickDy = 0f,
                currentSpeedKmh = 0f,
                isCruiseLocked = false
            )
        }
        val intent = Intent(context, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {
        }
    }

    private fun ensureFallbackLoopRunning() {
        if (fallbackLoopJob?.isActive == true) return
        fallbackLoopJob = scope.launch {
            var lastTickNs = System.nanoTime()
            while (isActive && _state.value.status != MockRunStatus.IDLE) {
                if (isServiceLoopActive) {
                    break
                }
                val intervalMs = latestSettings.updateIntervalMs.coerceIn(100L, 1000L)
                val nowNs = System.nanoTime()
                val deltaSec = ((nowNs - lastTickNs) / 1_000_000_000.0).coerceIn(0.01, 1.0)
                lastTickNs = nowNs
                stepSimulation(deltaSec)
                delay(intervalMs)
            }
        }
    }

    fun teleportTo(lat: Double, lng: Double, clearTrail: Boolean = false) {
        _state.update { current ->
            val clampedLat = lat.coerceIn(-85.0, 85.0)
            val clampedLng = lng.coerceIn(-180.0, 180.0)
            val newTrail = if (clearTrail) {
                listOf(GeoPointData(clampedLat, clampedLng))
            } else {
                (current.trail + GeoPointData(clampedLat, clampedLng)).takeLast(300)
            }
            current.copy(
                latitude = clampedLat,
                longitude = clampedLng,
                trail = newTrail
            )
        }
        val current = _state.value
        if (current.status == MockRunStatus.ACTIVE_SYSTEM) {
            engine.pushLocation(
                latitude = current.latitude,
                longitude = current.longitude,
                bearing = current.bearing,
                speedMps = 0f,
                enableJitter = latestSettings.enableGpsJitter
            )
        }
    }

    fun updateJoystickVector(dx: Float, dy: Float) {
        _state.update { current ->
            if (current.isCruiseLocked && hypot(dx.toDouble(), dy.toDouble()) < 0.05) {
                current
            } else {
                current.copy(
                    joystickDx = dx.coerceIn(-1f, 1f),
                    joystickDy = dy.coerceIn(-1f, 1f)
                )
            }
        }
    }

    fun toggleCruiseLock() {
        _state.update { current ->
            val nextLock = !current.isCruiseLocked
            if (!nextLock) {
                current.copy(
                    isCruiseLocked = false,
                    joystickDx = 0f,
                    joystickDy = 0f,
                    currentSpeedKmh = 0f
                )
            } else {
                val mag = hypot(current.joystickDx.toDouble(), current.joystickDy.toDouble())
                if (mag < 0.1) {
                    current.copy(
                        isCruiseLocked = true,
                        joystickDx = 0f,
                        joystickDy = -1f
                    )
                } else {
                    current.copy(isCruiseLocked = true)
                }
            }
        }
    }

    fun clearTrail() {
        _state.update { current ->
            current.copy(trail = listOf(GeoPointData(current.latitude, current.longitude)))
        }
    }

    fun stepSimulation(deltaTimeSec: Double) {
        val current = _state.value
        if (current.status == MockRunStatus.IDLE) return

        val settings = latestSettings
        val mag = hypot(current.joystickDx.toDouble(), current.joystickDy.toDouble()).coerceIn(0.0, 1.0)
        val isMoving = mag >= 0.04

        var nextLat = current.latitude
        var nextLng = current.longitude
        var nextBearing = current.bearing
        var speedMps = 0f
        var speedKmh = 0f

        if (isMoving) {
            val maxMps = settings.activeSpeedMetersPerSec
            val (lat2, lng2, bearingDeg) = GeoMath.advanceCoordinate(
                lat = current.latitude,
                lng = current.longitude,
                dx = current.joystickDx,
                dy = current.joystickDy,
                maxSpeedMetersPerSec = maxMps,
                deltaSeconds = deltaTimeSec
            )
            nextLat = lat2
            nextLng = lng2
            nextBearing = bearingDeg
            speedMps = (maxMps * mag).toFloat()
            speedKmh = (settings.activeSpeedKmh * mag).toFloat()
        }

        if (current.status == MockRunStatus.ACTIVE_SYSTEM) {
            val injected = engine.pushLocation(
                latitude = nextLat,
                longitude = nextLng,
                bearing = nextBearing,
                speedMps = speedMps,
                enableJitter = settings.enableGpsJitter
            )
            if (!injected && !engine.isMockAppAuthorized()) {
                _state.update {
                    it.copy(
                        status = MockRunStatus.ACTIVE_PREVIEW,
                        isDeveloperMockAuthorized = false
                    )
                }
            }
        }

        _state.update { stateNow ->
            val updatedTrail = if (isMoving) {
                val last = stateNow.trail.lastOrNull()
                if (last == null || GeoMath.distanceMeters(last.latitude, last.longitude, nextLat, nextLng) >= 2.0) {
                    (stateNow.trail + GeoPointData(nextLat, nextLng)).takeLast(300)
                } else {
                    stateNow.trail
                }
            } else {
                stateNow.trail
            }
            stateNow.copy(
                latitude = nextLat,
                longitude = nextLng,
                bearing = nextBearing,
                currentSpeedKmh = speedKmh,
                trail = updatedTrail
            )
        }
    }
}
