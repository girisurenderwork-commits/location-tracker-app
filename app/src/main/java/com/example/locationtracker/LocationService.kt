package com.example.locationtracker

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var ntfyTopic: String = ""
    private val channelId = "location_tracker_channel"
    private val notificationId = 42

    private val scope = CoroutineScope(Dispatchers.IO)

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            sendLocation(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ntfyTopic = intent?.getStringExtra("ntfy_topic") ?: ""

        startForeground(notificationId, buildNotification("Starting location tracking..."))

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Every 30 seconds, GPS for best accuracy (typically 3-8m outdoors).
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                30_000L,
                0f,
                locationListener
            )
            // Network provider as a backup source, e.g. indoors when GPS has no fix yet.
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    30_000L,
                    0f,
                    locationListener
                )
            } catch (_: Exception) {
                // Network provider may not exist on all devices; safe to ignore
            }
            // Immediately try the last known fix so the map isn't empty while waiting
            // for the first fresh reading.
            try {
                val last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (last != null && System.currentTimeMillis() - last.time < 5 * 60_000) {
                    sendLocation(last)
                }
            } catch (_: Exception) {}
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    private var bestAccuracySoFar = Float.MAX_VALUE

    private fun sendLocation(location: Location) {
        // Discard clearly noisy fixes (e.g. a bad network-provider guess) unless it's
        // the only reading we've gotten in a while.
        if (location.accuracy > 100f && location.accuracy > bestAccuracySoFar * 3) return
        bestAccuracySoFar = minOf(bestAccuracySoFar, location.accuracy)

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6 else null
        val message = "Lat: ${location.latitude}, Lon: ${location.longitude}, " +
                "Accuracy: ${location.accuracy}m, Time: $timestamp" +
                (speedKmh?.let { ", Speed: %.1fkm/h".format(it) } ?: "")

        updateNotification(message)

        if (ntfyTopic.isBlank()) return

        scope.launch {
            try {
                val url = URL("https://ntfy.sh/$ntfyTopic")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Title", "Location update")
                conn.outputStream.use { it.write(message.toByteArray()) }
                conn.responseCode // triggers the request
                conn.disconnect()
            } catch (_: Exception) {
                // Network error — next update will try again in ~60s
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Tracker running")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
