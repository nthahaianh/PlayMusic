package com.example.appmusic

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.appmusic.MyApplication.Companion.CHANNEL_ID
import kotlin.random.Random

class MyService : Service() {
    companion object {
        var songs: MutableList<MySong> = mutableListOf()
        var mediaPlayer = MediaPlayer()
        const val ON_PAUSE = 11
        const val ON_START = 12
        const val ON_RESUME = 13
        const val ON_STOP = 14
        const val ON_PREVIOUS = 15
        const val ON_NEXT = 16
        const val ON_SHUFFLE = 17
        const val ON_UN_SHUFFLE = 18
        const val ON_TIME = 19
    }
    lateinit var mSong: MySong
    var typeRepeat: Int = 0
    lateinit var sharedPreferences: SharedPreferences
    override fun onBind(intent: Intent): IBinder {
        return mybinder
    }

    private val mybinder = MyBinder()

    inner class MyBinder : Binder() {
        fun getMyService(): MyService = this@MyService
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = this.getSharedPreferences("SharePreferences", Context.MODE_PRIVATE)
        loadSongs()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mediaPlayer!=null){
            mediaPlayer.stop()
        }
        val editor = sharedPreferences.edit()
        editor.putInt("startOrResume", ON_START)
        editor.putBoolean("isPlaySong", false)
        editor.apply()
        Log.e("MyService", "Destroy")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var titleName = intent.getIntExtra("index", 0)
        if (songs.size<1|| songs ==null){
            loadSongs()
        }else
        sendNotification(songs[titleName])
        var action=intent.getIntExtra("action",0)
        manageMusic(action)
        return START_NOT_STICKY
    }

    fun manageMusic(action:Int){
        when (action) {
            ON_START -> {
                startSong()
            }
            ON_PAUSE -> {
                pauseSong()
            }
            ON_STOP -> {
                stopSelf()
                sendActiontoActivity(ON_STOP)
            }
            ON_RESUME -> {
                resumeSong()
            }
            ON_PREVIOUS -> {
                playPreviousSong()
            }
            ON_NEXT -> {
                playNextSong()
            }
            ON_SHUFFLE -> {
                shuffleSongs()
            }
            ON_UN_SHUFFLE -> {
                reloadSongs()
            }
        }
    }

    val handler = Handler(Looper.getMainLooper())
    val runnable = object : Runnable {
        override fun run() {
            var currentPos = mediaPlayer.currentPosition
            Log.e("runnable","$currentPos")
            val editor = sharedPreferences.edit()
            editor.putInt("currentProgress", currentPos)
            editor.apply()
            sendActiontoActivity(ON_TIME)
            handler.postDelayed(this, 1000)
        }
    }
    fun startSong() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        var currentSongIndex = sharedPreferences.getInt("currentSongIndex", 0)
        if (mediaPlayer.isPlaying) {
            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer()
        mSong = songs[currentSongIndex]
        mediaPlayer.setDataSource(this, Uri.parse(mSong.data))
        mediaPlayer.prepare()
        mediaPlayer.start()
        handler.postDelayed(runnable,1000)
        sendNotification(songs[currentSongIndex])
        val editor = sharedPreferences.edit()
        editor.putBoolean("isPlaySong", true)
        editor.apply()
        mediaPlayer.setOnCompletionListener {
            typeRepeat = sharedPreferences.getInt("typeRepeat", 0)
            when (typeRepeat) {
                0 -> {
                    mediaPlayer.stop()
                }
                1 -> {
                    startSong()
                }
                2 -> {
                    playNextSong()
                }
            }
        }
        sendNotification(songs[currentSongIndex])
        sendActiontoActivity(ON_START)
    }
    fun pauseSong() {
        try{
            var isPlaying = sharedPreferences.getBoolean("isPlaySong",false)
            if(mediaPlayer!=null && isPlaying){
                mediaPlayer.pause()
            }
            val editor = sharedPreferences.edit()
            editor.putBoolean("isPlaySong", false)
            var currentProgress = mediaPlayer!!.currentPosition
            editor.putInt("currentProgress",currentProgress)
            editor.apply()
            var currentSongIndex = sharedPreferences.getInt("currentSongIndex", 0)
            sendNotification(songs[currentSongIndex])
            sendActiontoActivity(ON_PAUSE)
        } catch (ex: Exception) {
            ex.stackTrace
        }
    }

    private fun resumeSong() {
        var isPlaying = sharedPreferences.getBoolean("isPlaySong",false)
        if (mediaPlayer==null)
            startSong()
        if(mediaPlayer!=null && !isPlaying){
            mediaPlayer.start()
            val editor = sharedPreferences.edit()
            editor.putBoolean("isPlaySong", true)
            editor.apply()
            var currentSongIndex = sharedPreferences.getInt("currentSongIndex", 0)
            sendNotification(songs[currentSongIndex])
            sendActiontoActivity(ON_RESUME)
        }
    }

    fun playNextSong(){
        var currentSongIndex = sharedPreferences.getInt("currentSongIndex", 0)
        currentSongIndex++
        if (currentSongIndex >= (songs.size))
            currentSongIndex = 0
        var isShuffle = sharedPreferences.getBoolean("isShuffle", false)
        if(isShuffle){
            currentSongIndex = Random.nextInt(songs.size-1)
        }
        val editor = sharedPreferences.edit()
        editor.putInt("currentSongIndex", currentSongIndex)
        editor.apply()
        Log.e("Services-playNextSong", "currentSongIndex=$currentSongIndex")
        startSong()
    }
    fun playPreviousSong(){
        var currentSongIndex = sharedPreferences.getInt("currentSongIndex", 0)
        if (currentSongIndex == 0)
            currentSongIndex = (songs.size-1)
        else {
            currentSongIndex--
        }
        val editor = sharedPreferences.edit()
        editor.putInt("currentSongIndex", currentSongIndex)
        editor.apply()
        Log.e("Services-playPrevious", "currentSongIndex=$currentSongIndex")
        startSong()

    }
    private fun sendActiontoActivity(action: Int){
        var intent = Intent("ac_service_to_main")
        var bundle=Bundle()
//        var currentSongIndex = sharedPreferences.getInt("currentSongIndex", 0)
//        bundle.putSerializable("obj_song",songs[currentSongIndex])
        bundle.putInt("action",action)
        intent.putExtras(bundle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendNotification(song: MySong) {
        val remoteViews = RemoteViews(packageName, R.layout.notification)
        remoteViews.setTextViewText(R.id.notification_tvTitle, "${song.title}")
        remoteViews.setTextViewText(R.id.notification_tvText, "${song.artist}")
        if(mediaPlayer.isPlaying){
            remoteViews.setOnClickPendingIntent(R.id.notification_btnPlay,getPendingIntent(this,
                ON_PAUSE
            ))
            remoteViews.setImageViewResource(R.id.notification_btnPlay,R.drawable.ic_pause)
        }else{
            remoteViews.setOnClickPendingIntent(R.id.notification_btnPlay,getPendingIntent(this,
                ON_RESUME
            ))
            remoteViews.setImageViewResource(R.id.notification_btnPlay,R.drawable.ic_play_arrow)
        }
        remoteViews.setOnClickPendingIntent(R.id.notification_btnNext_song,getPendingIntent(this,
            ON_NEXT
        ))
        remoteViews.setOnClickPendingIntent(R.id.notification_btnPrevious_song,getPendingIntent(this,
            ON_PREVIOUS
        ))
        remoteViews.setOnClickPendingIntent(R.id.notification_btnClose,getPendingIntent(this, ON_STOP))
        var intent = Intent(this, MainActivity::class.java)
        var pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        var notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.music)
            .setContentIntent(pendingIntent)
            .setCustomContentView(remoteViews)
        startForeground(1, notification.build())
    }

    fun getPendingIntent(context: Context,action:Int):PendingIntent{
        var intent=Intent(this, MyReceiver::class.java)
        intent.putExtra("action",action)
        return PendingIntent.getBroadcast(context.applicationContext,action,intent,PendingIntent.FLAG_UPDATE_CURRENT)
    }


    private fun shuffleSongs() {
        songs.shuffle()
        Log.d(
            "Service - shuffle",
            "----------------------------------Shuffle-----------------------------------"
        )
    }

    private fun reloadSongs(){
        songs.clear()
        loadSongs()
    }
    private fun loadSongs() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )
        while (cursor!!.moveToNext()) {
            songs.add(
                MySong(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getLong(5)
                )
            )
//            if (cursor.getLong(5)>0){
//                songs.add(
//                    MySong(
//                        cursor.getString(0),
//                        cursor.getString(1),
//                        cursor.getString(2),
//                        cursor.getString(3),
//                        cursor.getString(4),
//                        cursor.getLong(5)
//                    )
//                )
//            }
        }
        for(song in songs){
            Log.e("Sv-Song","$song")
        }
    }
}