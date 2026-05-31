package com.example.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "sound_alerts_channel"
    private const val CHANNEL_NAME = "Decibel Hazard Warnings"
    private const val CHANNEL_DESC = "Triggers alerts when entering/detecting high-noise regions."

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendNoiseAlert(context: Context, locationName: String, dbLevel: Int) {
        // Build the system notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ CRITICAL DECIBEL EXPOSURE!")
            .setContentText("Zone Red alert detected near '$locationName': logged at ${dbLevel}dB. Noise Sentry advises wearing audio insulation gear.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("An extreme disturbance of ${dbLevel}dB has triggered community sentry parameters in '$locationName'. High exposure risks acoustic impact! Please exercise safety caution."))

        try {
            with(NotificationManagerCompat.from(context)) {
                notify((1000..9999).random(), builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted or exception
            e.printStackTrace()
        }
    }
}
