package com.example.location

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPointData(
    val latitude: Double,
    val longitude: Double
)

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Advances a coordinate by a joystick vector (dx, dy) where:
     * dx in [-1, 1] (+ is East, - is West)
     * dy in [-1, 1] (- is North/Up on screen, + is South/Down on screen)
     */
    fun advanceCoordinate(
        lat: Double,
        lng: Double,
        dx: Float,
        dy: Float,
        maxSpeedMetersPerSec: Double,
        deltaSeconds: Double
    ): Triple<Double, Double, Float> {
        val magnitude = hypot(dx.toDouble(), dy.toDouble()).coerceIn(0.0, 1.0)
        if (magnitude < 0.04 || deltaSeconds <= 0.0) {
            return Triple(lat, lng, 0f)
        }

        // Screen dy < 0 means North (0 deg), dx > 0 means East (90 deg)
        val bearingRad = atan2(dx.toDouble(), -dy.toDouble())
        val bearingDeg = ((Math.toDegrees(bearingRad) + 360.0) % 360.0).toFloat()

        val currentSpeed = maxSpeedMetersPerSec * magnitude
        val distanceMeters = currentSpeed * deltaSeconds
        val angularDistance = distanceMeters / EARTH_RADIUS_METERS

        val lat1 = Math.toRadians(lat)
        val lng1 = Math.toRadians(lng)

        val lat2 = asin(
            sin(lat1) * cos(angularDistance) +
                cos(lat1) * sin(angularDistance) * cos(bearingRad)
        )
        val lng2 = lng1 + atan2(
            sin(bearingRad) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2)
        )

        val newLat = Math.toDegrees(lat2).coerceIn(-85.0, 85.0)
        val newLng = ((Math.toDegrees(lng2) + 540.0) % 360.0) - 180.0
        return Triple(newLat, newLng, bearingDeg)
    }

    fun formatCoords(lat: Double, lng: Double): String {
        return String.format(java.util.Locale.US, "%.6f, %.6f", lat, lng)
    }

    fun compassLabelIndonesian(bearingDeg: Float): String {
        val normalized = ((bearingDeg % 360f) + 360f) % 360f
        return when {
            normalized < 22.5f || normalized >= 337.5f -> "U"
            normalized < 67.5f -> "TL"
            normalized < 112.5f -> "T"
            normalized < 157.5f -> "TG"
            normalized < 202.5f -> "S"
            normalized < 247.5f -> "BD"
            normalized < 292.5f -> "B"
            else -> "BL"
        }
    }

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
