package com.example

import android.app.Application
import com.example.data.GeoJoyDatabase
import com.example.data.LocationRepository
import com.example.data.SettingsRepository
import com.example.location.MockLocationController
import org.osmdroid.config.Configuration

class GeoJoyApplication : Application() {

    lateinit var database: GeoJoyDatabase
        private set

    lateinit var locationRepository: LocationRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var mockLocationController: MockLocationController
        private set

    override fun onCreate() {
        super.onCreate()

        // Load osmdroid defaults first, then override paths to app-private directories with mkdirs()
        val osmdroidPrefs = getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
        val baseDir = filesDir.resolve("osmdroid").apply { mkdirs() }
        val tileCacheDir = cacheDir.resolve("osmdroid_tiles").apply { mkdirs() }
        Configuration.getInstance().apply {
            load(this@GeoJoyApplication, osmdroidPrefs)
            userAgentValue = "$packageName/1.0"
            osmdroidBasePath = baseDir
            osmdroidTileCache = tileCacheDir
            tileDownloadThreads = 4
            tileFileSystemThreads = 4
            tileDownloadMaxQueueSize = 40
        }

        database = GeoJoyDatabase.getInstance(this)
        locationRepository = LocationRepository(database.locationDao())
        settingsRepository = SettingsRepository(this)
        mockLocationController = MockLocationController(this, settingsRepository)
    }
}
