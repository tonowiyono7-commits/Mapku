package com.example.location

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.random.Random

class MockLocationEngine(private val context: Context) {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var gpsTestProviderReady = false
    private var fusedMockActive = false

    fun isMockAppAuthorized(): Boolean {
        return try {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ops.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun startMockProviders(): Boolean {
        var anySuccess = false
        try {
            ensureGpsTestProvider()
            gpsTestProviderReady = true
            anySuccess = true
        } catch (_: SecurityException) {
            gpsTestProviderReady = false
        } catch (_: Exception) {
            gpsTestProviderReady = false
        }

        try {
            fusedClient.setMockMode(true)
                .addOnSuccessListener { fusedMockActive = true }
                .addOnFailureListener { fusedMockActive = false }
        } catch (_: SecurityException) {
            fusedMockActive = false
        } catch (_: Exception) {
            fusedMockActive = false
        }

        return anySuccess || fusedMockActive
    }

    @SuppressLint("MissingPermission")
    fun pushLocation(
        latitude: Double,
        longitude: Double,
        bearing: Float,
        speedMps: Float,
        enableJitter: Boolean
    ): Boolean {
        val jitterLat = if (enableJitter) (Random.nextDouble(-0.5, 0.5) / 111_111.0) else 0.0
        val jitterLng = if (enableJitter) (Random.nextDouble(-0.5, 0.5) / 111_111.0) else 0.0

        val targetLat = (latitude + jitterLat).coerceIn(-85.0, 85.0)
        val targetLng = (longitude + jitterLng).coerceIn(-180.0, 180.0)

        val mockLocation = Location(LocationManager.GPS_PROVIDER).apply {
            this.latitude = targetLat
            this.longitude = targetLng
            this.altitude = 18.0 + (if (enableJitter) Random.nextDouble(-0.4, 0.4) else 0.0)
            this.accuracy = if (enableJitter) (3.2f + Random.nextFloat() * 1.6f) else 3.0f
            this.bearing = bearing
            this.speed = speedMps
            this.time = System.currentTimeMillis()
            this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.bearingAccuracyDegrees = 2.5f
                this.speedAccuracyMetersPerSecond = 0.5f
                this.verticalAccuracyMeters = 2.0f
            }
        }

        var injected = false
        if (gpsTestProviderReady) {
            try {
                locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
                injected = true
            } catch (_: SecurityException) {
                gpsTestProviderReady = false
            } catch (_: IllegalArgumentException) {
                // Provider may have been removed; try re-adding once
                try {
                    ensureGpsTestProvider()
                    locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
                    injected = true
                } catch (_: Exception) {
                    gpsTestProviderReady = false
                }
            } catch (_: Exception) {
            }
        }

        if (fusedMockActive) {
            try {
                fusedClient.setMockLocation(mockLocation)
                    .addOnFailureListener { fusedMockActive = false }
                injected = true
            } catch (_: SecurityException) {
                fusedMockActive = false
            } catch (_: Exception) {
            }
        }

        return injected
    }

    @SuppressLint("MissingPermission")
    fun stopMockProviders() {
        if (gpsTestProviderReady) {
            try {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
            } catch (_: Exception) {
            }
            try {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
            } catch (_: Exception) {
            }
            gpsTestProviderReady = false
        }

        if (fusedMockActive) {
            try {
                fusedClient.setMockMode(false)
            } catch (_: Exception) {
            }
            fusedMockActive = false
        }
    }

    @Suppress("DEPRECATION")
    private fun ensureGpsTestProvider() {
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val properties = ProviderProperties.Builder()
                .setHasNetworkRequirement(false)
                .setHasSatelliteRequirement(false)
                .setHasCellRequirement(false)
                .setHasMonetaryCost(false)
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                .setAccuracy(ProviderProperties.ACCURACY_FINE)
                .build()
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, properties)
        } else {
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE
            )
        }
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
    }
}
