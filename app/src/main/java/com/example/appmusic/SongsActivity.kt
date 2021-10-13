package com.example.appmusic

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmusic.MyService.Companion.ON_NEXT
import com.example.appmusic.MyService.Companion.ON_PAUSE
import com.example.appmusic.MyService.Companion.ON_PREVIOUS
import com.example.appmusic.MyService.Companion.ON_RESUME
import com.example.appmusic.MyService.Companion.ON_START
import com.example.appmusic.MyService.Companion.ON_STOP
import kotlinx.android.synthetic.main.activity_songs.*

class SongsActivity : AppCompatActivity() {
    var songs: MutableList<MySong> = mutableListOf()
    lateinit var sharedPreferences: SharedPreferences
    var indexCurrentSong: Int = 0
    var isPlaySong: Boolean = false
    var startOrResume= ON_START

    private val myBroadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val bundle = intent!!.extras ?: return
            var action=bundle.getInt("action")
            updateUI(action)
        }
    }

    private fun updateUI(action: Int) {
        when (action) {
            ON_START -> {
                indexCurrentSong = sharedPreferences.getInt("currentSongIndex", 0)
                isPlaySong = sharedPreferences.getBoolean("isPlaySong", false)
                updateBtnPlay()
                songs= MyService.songs
                var nowSong = songs[indexCurrentSong]
                list_song_tvTitle.text = nowSong.title
            }
            ON_PAUSE -> {
                isPlaySong = sharedPreferences.getBoolean("isPlaySong", false)
                updateBtnPlay()
            }
            ON_STOP -> {
                isPlaySong = sharedPreferences.getBoolean("isPlaySong", false)
                updateBtnPlay()
                startOrResume = ON_START
                val editor = sharedPreferences.edit()
                editor.putInt("startOrResume", startOrResume)
                editor.apply()
            }
            ON_RESUME -> {
                isPlaySong = sharedPreferences.getBoolean("isPlaySong", false)
                updateBtnPlay()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_songs)
        songs= MyService.songs
        if (songs.isEmpty()) loadSongs()
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcast, IntentFilter("ac_service_to_main"))
        sharedPreferences = this.getSharedPreferences("SharePreferences", Context.MODE_PRIVATE)
        indexCurrentSong = sharedPreferences.getInt("currentSongIndex",0)
        isPlaySong = sharedPreferences.getBoolean("isPlaySong",false)
        startOrResume = sharedPreferences.getInt("startOrResume", ON_START)
        updateBtnPlay()
        try{
            list_song_tvTitle.text = songs[indexCurrentSong].title
        } catch (e:Exception){
            Log.e("Songs","${songs.size}  -  ${songs[indexCurrentSong]}")
            e.stackTrace
        }
        list_song_btnPlay.setOnClickListener {
            if (isPlaySong){
                val intent = Intent(this, MyService::class.java)
                intent.putExtra("action", ON_PAUSE)
                startService(intent)
            }else{
                val intent = Intent(this, MyService::class.java)
                intent.putExtra("action", startOrResume)
                startService(intent)
                startOrResume = ON_RESUME
                val editor = sharedPreferences.edit()
                editor.putInt("startOrResume", startOrResume)
                editor.apply()
            }
            isPlaySong = !isPlaySong
            updateBtnPlay()
        }
        list_song_btnPrevious_song.setOnClickListener {
            val intent = Intent(this, MyService::class.java)
            intent.putExtra("action", ON_PREVIOUS)
            startService(intent)
        }
        list_song_btnNext_song.setOnClickListener {
            val intent = Intent(this, MyService::class.java)
            intent.putExtra("action", ON_NEXT)
            startService(intent)
        }
        val layoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(baseContext, LinearLayoutManager.VERTICAL, false)
        var adapter = SongAdapter(songs)
        list_song_rvSongs.layoutManager = layoutManager
        list_song_rvSongs.adapter = adapter
        adapter.setCallBack {
            list_song_tvTitle.text = songs[it].title
            indexCurrentSong=it
            val editor = sharedPreferences.edit()
            editor.putInt("currentSongIndex",indexCurrentSong)
            startOrResume = ON_RESUME
            editor.putInt("startOrResume", startOrResume)
            editor.apply()
            val intent = Intent(this, MyService::class.java)
            stopService(intent)
            intent.putExtra("action",ON_START)
            startService(intent)
        }

        list_song_tvTitle.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putInt("currentSongIndex",indexCurrentSong)
            editor.apply()
            finish()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadcast)
    }
    private fun updateBtnPlay() {
        if (isPlaySong) {
            list_song_btnPlay.setImageResource(R.drawable.ic_pause)
        } else {
            list_song_btnPlay.setImageResource(R.drawable.ic_play_arrow)
        }
    }
    private fun loadSongs() {
        Log.e("SongsAct","Load songs")
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
            if (cursor.getLong(5)>0){
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
            }
        }
    }

}