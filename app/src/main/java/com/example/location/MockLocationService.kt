package com.example.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.GeoJoyApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MockLocationService : Service() {

    companion object {
        const val ACTION_START = "com.example.location.ACTION_START"
        const val ACTION_STOP = "com.example.location.ACTION_STOP"
        private const val CHANNEL_ID = "geojoy_mock_gps_channel"
        private const val NOTIFICATION_ID = 1042
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    private val controller: MockLocationController
        get() = (application as GeoJoyApplication).mockLocationController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopLoopAndService()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                if (!hasLocationPermission()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val notification = buildNotification(controller.state.value)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (_: Exception) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startMockLoop()
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startMockLoop() {
        if (loopJob?.isActive == true) return
        controller.isServiceLoopActive = true
        loopJob = serviceScope.launch {
            var lastTickNs = System.nanoTime()
            var lastNotifMs = 0L
            try {
                while (isActive) {
                    val intervalMs = controller.latestSettings.updateIntervalMs.coerceIn(100L, 1000L)
                    val nowNs = System.nanoTime()
                    val deltaSec = ((nowNs - lastTickNs) / 1_000_000_000.0).coerceIn(0.01, 1.0)
                    lastTickNs = nowNs

                    val state = controller.state.value
                    if (state.status == MockRunStatus.IDLE) {
                        break
                    }

                    controller.stepSimulation(deltaSec)

                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastNotifMs > 1800L) {
                        lastNotifMs = nowMs
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(NOTIFICATION_ID, buildNotification(controller.state.value))
                    }

                    delay(intervalMs)
                }
            } finally {
                controller.isServiceLoopActive = false
            }
        }
    }

    private fun stopLoopAndService() {
        controller.isServiceLoopActive = false
        loopJob?.cancel()
        loopJob = null
        if (controller.state.value.status != MockRunStatus.IDLE) {
            controller.stopSpoofing()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: MockLocationState): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MockLocationService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusTitle = if (state.status == MockRunStatus.ACTIVE_SYSTEM) {
            getString(R.string.status_active_system)
        } else {
            getString(R.string.status_active_preview)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_geojoy)
            .setContentTitle("GeoJoy • $statusTitle")
            .setContentText(GeoMath.formatCoords(state.latitude, state.longitude))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_stop), stopIntent)
            .build()
    }

    override fun onDestroy() {
        controller.isServiceLoopActive = false
        loopJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
