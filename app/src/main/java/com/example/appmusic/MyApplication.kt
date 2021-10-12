package com.example.appmusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotification()
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            var channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setSound(null,null)
            var manager = getSystemService(NotificationManager::class.java)
            if (manager!=null){
                manager.createNotificationChannel(channel)
            }
        }

    }

    companion object {
        const val CHANNEL_NAME = "channel_name"
        const val CHANNEL_ID = "channel_id"
    }
}