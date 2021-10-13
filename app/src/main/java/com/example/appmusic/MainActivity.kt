package com.example.appmusic

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.appmusic.MyService.Companion.ON_NEXT
import com.example.appmusic.MyService.Companion.ON_PAUSE
import com.example.appmusic.MyService.Companion.ON_START
import com.example.appmusic.MyService.Companion.ON_PREVIOUS
import com.example.appmusic.MyService.Companion.ON_RESUME
import com.example.appmusic.MyService.Companion.ON_SHUFFLE
import com.example.appmusic.MyService.Companion.ON_UN_SHUFFLE
import com.example.appmusic.MyService.Companion.mediaPlayer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE = 1
    var songs: MutableList<MySong> = mutableListOf()
    lateinit var sharedPreferences: SharedPreferences
    var indexCurrentSong: Int = 0
    var typeRepeat: Int = 0
    var isShuffle = false
    var isPlaySong: Boolean = false
    var startOrResume= ON_START
    private var isConnected: Boolean = false

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
                main_tvSong.text = nowSong.title

                var currentProgress = sharedPreferences.getInt("currentProgress", 0)
                val totalDuration = nowSong.duration
                var currentPos = currentProgress
                main_tvMaxTime.text = millionSecondsToTime(totalDuration)
                main_tvCurrentTime.text = intToTime(currentPos)
                seekBar.max = totalDuration.toInt()
                val handler = Handler(Looper.getMainLooper())
                val runnable = object : Runnable {
                    override fun run() {
                        currentPos = mediaPlayer.currentPosition
                        main_tvCurrentTime.text = intToTime(currentPos)
                        seekBar.progress = currentPos
                        handler.postDelayed(this, 1000)
                    }
                }
                handler.postDelayed(runnable, 1000)
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {

                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        mediaPlayer.seekTo(seekBar.progress)
                    }

                })
            }
            ON_PAUSE -> {
                isPlaySong = sharedPreferences.getBoolean("isPlaySong", false)
                updateBtnPlay()

            }
            MyService.ON_STOP -> {
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
                var index = sharedPreferences.getInt("currentSongIndex", 0)
                isPlaySong = sharedPreferences.getBoolean("isPlaySong", false)
                songs= MyService.songs
                var nowSong = songs[index]
                main_tvSong.text = nowSong.title

                var currentProgress = sharedPreferences.getInt("currentProgress", 0)
                val totalDuration = nowSong.duration
                var currentPos = currentProgress
                main_tvMaxTime.text = millionSecondsToTime(totalDuration)
                main_tvCurrentTime.text = intToTime(currentPos)
                seekBar.max = totalDuration.toInt()
                val handler = Handler(Looper.getMainLooper())
                val runnable = object : Runnable {
                    override fun run() {
                        currentPos = mediaPlayer.currentPosition
                        main_tvCurrentTime.text = intToTime(currentPos)
                        seekBar.progress = currentPos
                        handler.postDelayed(this, 1000)
                    }
                }
                handler.postDelayed(runnable, 1000)
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {

                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        mediaPlayer.seekTo(seekBar.progress)
                    }

                })
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcast, IntentFilter("ac_service_to_main"))
        requestPermission()
        sharedPreferences = this.getSharedPreferences("SharePreferences", Context.MODE_PRIVATE)
        indexCurrentSong = sharedPreferences.getInt("currentSongIndex", 0)
        typeRepeat = sharedPreferences.getInt("typeRepeat", 0)
        isShuffle = sharedPreferences.getBoolean("isShuffle", false)
        isPlaySong = sharedPreferences.getBoolean("isPlaySong", false)
        startOrResume = sharedPreferences.getInt("startOrResume", ON_START)
        Log.e("Log","$indexCurrentSong,$typeRepeat,$isShuffle,$isPlaySong,$isConnected")
        try{
            songs= MyService.songs
            main_tvSong.text = songs[indexCurrentSong].title
            Log.e("Log2", "${songs.size}-${songs[indexCurrentSong].title}")
        }catch (e:Exception){
            e.stackTrace
        }
        updateIconRepeat()
        updateBtnPlay()
        updateIconShuffle()
        main_ivShuffle.setOnClickListener {
            isShuffle = !isShuffle
            updateIconShuffle()
        }
        main_ivTypeRepeat.setOnClickListener {
            when (typeRepeat) {
                2 -> typeRepeat = 0
                else ->typeRepeat++
            }
            updateIconRepeat()
        }
        main_btnPlay.setOnClickListener {
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
        main_btnNext_song.setOnClickListener {
            val intent = Intent(this, MyService::class.java)
            intent.putExtra("action", ON_NEXT)
            startService(intent)
        }
        main_btnPrevious_song.setOnClickListener {
            val intent = Intent(this, MyService::class.java)
            intent.putExtra("action", ON_PREVIOUS)
            startService(intent)
        }
        main_ivPlaylist.setOnClickListener {
            val intent = Intent(this, SongsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateBtnPlay() {
        if (isPlaySong) {
            main_btnPlay.setImageResource(R.drawable.ic_pause)
        } else {
            main_btnPlay.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    private fun updateIconRepeat() {
        when (typeRepeat) {
            0 -> {
                main_ivTypeRepeat.setImageResource(R.drawable.ic_no_repeat)
            }
            1 -> {
                main_ivTypeRepeat.setImageResource(R.drawable.ic_repeat_one)
            }
            2 -> {
                main_ivTypeRepeat.setImageResource(R.drawable.ic_repeat)
            }
        }
        val editor = sharedPreferences.edit()
        editor.putInt("typeRepeat", typeRepeat)
        editor.apply()
    }

    private fun updateIconShuffle() {
        if (isShuffle) {
            main_ivShuffle.setImageResource(R.drawable.ic__shuffle_choose)
        } else {
            main_ivShuffle.setImageResource(R.drawable.ic_shuffle)
        }
        val editor = sharedPreferences.edit()
        editor.putBoolean("isShuffle", isShuffle)
        editor.apply()
    }
    fun millionSecondsToTime(milliSeconds: Long): String {
        val hours = milliSeconds / (1000*60*60)
        val minutes = (milliSeconds % (1000*60*60)) / (1000*60)
        val seconds = (milliSeconds % (1000*60*60)) % (1000*60)/1000
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    fun intToTime(milliSeconds: Int): String {
        val hours = milliSeconds / (1000*60*60)
        val minutes = (milliSeconds % (1000*60*60)) / (1000*60)
        val seconds = (milliSeconds % (1000*60*60)) % (1000*60)/1000
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadcast)
    }
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, "Please allow storage permission", Toast.LENGTH_SHORT)
                            .show()
                    } else {
//                        loadSongs()
//                        startService(Intent(this,MyService::class.java))
                    }
                }
            }
        }
    }

}