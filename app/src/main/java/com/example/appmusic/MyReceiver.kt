package com.example.appmusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.getIntExtra("action",0)
        val intent1 = Intent(context, MyService::class.java)
        intent1.putExtra("action",action)
        Log.e("Receiver","$action")
        context!!.startService(intent1)
    }
}