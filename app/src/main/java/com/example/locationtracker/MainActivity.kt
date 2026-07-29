package com.example.locationtracker

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var topicInput: EditText
    private lateinit var statusText: TextView

    private val fineLocationRequestCode = 1001
    private val backgroundLocationRequestCode = 1002
    private val notificationRequestCode = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("location_tracker_prefs", MODE_PRIVATE)
        topicInput = findViewById(R.id.topicInput)
        statusText = findViewById(R.id.statusText)

        topicInput.setText(prefs.getString("ntfy_topic", ""))

        findViewById<Button>(R.id.startButton).setOnClickListener {
            val topic = topicInput.text.toString().trim()
            if (topic.isEmpty()) {
                Toast.makeText(this, "Enter a topic name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("ntfy_topic", topic).apply()
            requestPermissionsThenStart()
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
            statusText.text = "Status: stopped"
        }
    }

    private fun requestPermissionsThenStart() {
        // Android 13+ needs a runtime notification permission for the foreground service notice
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationRequestCode
            )
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), fineLocationRequestCode
            )
            return
        }

        // Background location must be requested separately, after foreground is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                "On the next screen, choose \"Allow all the time\" so tracking keeps working in the background.",
                Toast.LENGTH_LONG
            ).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                backgroundLocationRequestCode
            )
            return
        }

        startTracking()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Whatever was just answered, re-run the permission chain to ask for the next one
        // (or start tracking if everything is already granted).
        requestPermissionsThenStart()
    }

    private fun startTracking() {
        val topic = prefs.getString("ntfy_topic", "") ?: ""
        val intent = Intent(this, LocationService::class.java)
        intent.putExtra("ntfy_topic", topic)
        ContextCompat.startForegroundService(this, intent)
        statusText.text = "Status: tracking (topic: $topic)"
    }
}
