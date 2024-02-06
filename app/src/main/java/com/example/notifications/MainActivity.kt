package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.notifications.ui.theme.NotificationsTheme
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class MainActivity : ComponentActivity(), SensorEventListener {
    private val CHANNEL_ID = "notification_channel"
    private val NOTIFICATION_ID = 101

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        // Initialize SensorManager and Light Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        setContent {
            NotificationsTheme {
                NotificationScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

  //  override fun onPause() {
        //super.onPause()
        //sensorManager.unregisterListener(this)
    //}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_LIGHT) {
                val lux = it.values[0] // Ambient light level in lux
                Log.d("LightSensor", "Lux: $lux")

                // Define your thresholds
                val DARK_THRESHOLD = 10.0f  // Example value, adjust as necessary
                val LIGHT_THRESHOLD = 200.0f  // Example value, adjust as necessary

                when {
                    lux <= DARK_THRESHOLD -> {
                        Log.d("LightSensor", "It's dark. Lux: $lux")
                        // It's dark
                        // Add your logic here for dark conditions
                    }
                    lux >= LIGHT_THRESHOLD -> {
                        Log.d("LightSensor", "It's bright. Lux: $lux")
                        showNotification(lux)
                        // It's bright
                        // Add your logic here for bright conditions
                    }
                    else -> {
                        Log.d("LightSensor", "Intermediate light level. Lux: $lux")
                        // The light level is in-between
                        // Handle intermediate light levels if needed
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implement if needed
    }

    @Composable
    fun NotificationScreen() {
        Column(modifier = Modifier.padding(16.dp)) {
            Greeting(name = "Android")
            Button(
                onClick = {
                    checkAndShowNotification()
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Show Notification")
            }
        }
    }

    private fun checkAndShowNotification() {
        // For Android 12 (API level 31) and above, check for POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permission is granted, you can show the notification
                    showNotification(10.123f)
                }
                else -> {
                    // Permission is not granted, request the permission
                    requestNotificationPermission()
                }
            }
        } else {
            // For versions below Android 12, POST_NOTIFICATIONS permission isn't needed
            showNotification(10.123f)
        }
    }


    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, show the notification
            showNotification(10.123f)
        } else {
            // Permission denied, handle the denial
        }
    }

    private fun requestNotificationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(lux: Float) { // Accept lux as parameter
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Light Level Notification")
            .setContentText("Current light level: $lux lux") // Display lux value in the notification
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello $name!")
    }
}

