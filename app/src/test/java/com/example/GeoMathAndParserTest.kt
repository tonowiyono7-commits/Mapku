package com.example

import com.example.location.GeoMath
import com.example.network.GeocodingClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathAndParserTest {

    @Test
    fun joystickNorthMovesLatitudePositive() {
        val startLat = -6.175392
        val startLng = 106.827153
        val (newLat, newLng, bearing) = GeoMath.advanceCoordinate(
            lat = startLat,
            lng = startLng,
            dx = 0f,
            dy = -1f, // Up on screen = North
            maxSpeedMetersPerSec = 10.0,
            deltaSeconds = 1.0
        )
        assertTrue(newLat > startLat)
        assertEquals(startLng, newLng, 1e-6)
        assertEquals(0f, bearing, 0.5f)
    }

    @Test
    fun joystickEastMovesLongitudePositive() {
        val startLat = -6.175392
        val startLng = 106.827153
        val (newLat, newLng, bearing) = GeoMath.advanceCoordinate(
            lat = startLat,
            lng = startLng,
            dx = 1f,
            dy = 0f,
            maxSpeedMetersPerSec = 10.0,
            deltaSeconds = 1.0
        )
        assertTrue(newLng > startLng)
        assertEquals(startLat, newLat, 1e-5)
        assertEquals(90f, bearing, 0.5f)
    }

    @Test
    fun parseCoordinatesSupportsCommaAndSpace() {
        val comma = GeocodingClient.parseCoordinates("-6.990432, 110.422974")
        assertNotNull(comma)
        assertEquals(-6.990432, comma!!.first, 1e-6)
        assertEquals(110.422974, comma.second, 1e-6)

        val space = GeocodingClient.parseCoordinates("-7.792576 110.365845")
        assertNotNull(space)

        val invalid = GeocodingClient.parseCoordinates("Monas Jakarta")
        assertNull(invalid)
    }
}
