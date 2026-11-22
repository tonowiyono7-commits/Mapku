package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NominatimPlace(
    @param:Json(name = "place_id") val placeId: Long = 0L,
    @param:Json(name = "display_name") val displayName: String = "",
    @param:Json(name = "lat") val lat: String = "0",
    @param:Json(name = "lon") val lon: String = "0",
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "type") val type: String? = null
) {
    val latitude: Double get() = lat.toDoubleOrNull() ?: 0.0
    val longitude: Double get() = lon.toDoubleOrNull() ?: 0.0

    val shortTitle: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: displayName.substringBefore(",").trim().ifBlank { "Lokasi Terpilih" }
}

@JsonClass(generateAdapter = true)
data class NominatimReverseResponse(
    @param:Json(name = "display_name") val displayName: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "address") val address: NominatimAddress? = null
) {
    fun toShortTitle(): String {
        val road = address?.road ?: address?.suburb ?: address?.neighbourhood
        val city = address?.city ?: address?.town ?: address?.county ?: address?.state
        return when {
            !name.isNullOrBlank() && !city.isNullOrBlank() -> "$name, $city"
            !road.isNullOrBlank() && !city.isNullOrBlank() -> "$road, $city"
            !city.isNullOrBlank() -> city
            !displayName.isNullOrBlank() -> displayName.split(",").take(2).joinToString(", ").trim()
            else -> "Titik Peta"
        }
    }
}

@JsonClass(generateAdapter = true)
data class NominatimAddress(
    @param:Json(name = "road") val road: String? = null,
    @param:Json(name = "neighbourhood") val neighbourhood: String? = null,
    @param:Json(name = "suburb") val suburb: String? = null,
    @param:Json(name = "city") val city: String? = null,
    @param:Json(name = "town") val town: String? = null,
    @param:Json(name = "county") val county: String? = null,
    @param:Json(name = "state") val state: String? = null,
    @param:Json(name = "country") val country: String? = null
)

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 6,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") language: String = "id,en"
    ): List<NominatimPlace>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") language: String = "id,en"
    ): NominatimReverseResponse
}

object GeocodingClient {
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "GeoJoy-Android-GPS-Tool/1.0")
                .build()
            chain.proceed(req)
        }
        .build()

    val api: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }

    /**
     * Parses raw "lat, lng" or "lat lng" strings directly without needing network.
     */
    fun parseCoordinates(input: String): Pair<Double, Double>? {
        val cleaned = input.trim().replace(";", ",")
        val parts = if (cleaned.contains(",")) {
            cleaned.split(",")
        } else {
            cleaned.split(Regex("\\s+"))
        }
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return null
        return lat to lng
    }
}
