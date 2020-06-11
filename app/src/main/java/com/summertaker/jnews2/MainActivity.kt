package com.summertaker.jnews2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*

const val STATE_RESUME_WINDOW = "resumeWindow"
const val STATE_RESUME_POSITION = "resumePosition"
const val STATE_PLAYER_FULLSCREEN = "playerFullscreen"
const val STATE_PLAYER_PLAYING = "playerOnPlay"

class MainActivity : AppCompatActivity() { //SwipeActivity() {

    private val permissionRequestCode = 1000

    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isFullscreen = false
    private var isPlayerPlaying = true
    private var mVideos: ArrayList<Video> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
            isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
            isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
        }

        //initGesture()

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //    .setAction("Action", null).show()
            goArticles()
        }

        dataSourceFactory = DefaultDataSourceFactory(
            applicationContext,
            Util.getUserAgent(applicationContext, applicationContext.getString(R.string.app_name))
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_RESUME_WINDOW, exoPlayer.currentWindowIndex)
        outState.putLong(STATE_RESUME_POSITION, exoPlayer.currentPosition)
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, isFullscreen)
        outState.putBoolean(STATE_PLAYER_PLAYING, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.download -> {
                goArticles()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkPermissions() {
        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermission()
        } else {
            onPermissionGranted()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            permissionRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            permissionRequestCode -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG)
                        .show()
                } else {
                    onPermissionGranted()
                }
                return
            }
        }
    }

    private fun onPermissionGranted() {
        //Toast.makeText(this, "initUI()", Toast.LENGTH_LONG).show()
        initPlayer()
        if (playerView != null) playerView.onResume()
    }

    private fun initPlayer() {
        val dataManager = DataManager(this)
        val videos = dataManager.getVideoFiles()
        if (videos.size > 1) {
            val seed = System.nanoTime()
            videos.shuffle(java.util.Random(seed))
        }
        mVideos.clear()
        mVideos.addAll(videos)

        //val index = Random.nextInt(0, mVideos.size)
        //val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
        //    .createMediaSource(mVideos[index].contentUri)

        val concatenatedSource = ConcatenatingMediaSource()
        for (video in mVideos) {
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(video.contentUri)
            concatenatedSource.addMediaSource(videoSource)
        }

        exoPlayer = SimpleExoPlayer.Builder(this).build()
        with(exoPlayer) {
            playWhenReady = isPlayerPlaying
            repeatMode = Player.REPEAT_MODE_ALL
            seekTo(currentWindow, playbackPosition)
            prepare(concatenatedSource, false, false)
        }
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) { //플레이어 상태 변화를 받을 수 있음
                //Log.e(">>", playbackState.toString())
                when (playbackState) {
                    Player.STATE_IDLE -> {
                    }
                    Player.STATE_BUFFERING -> {
                    }
                    Player.STATE_READY -> {
                        val index = exoPlayer.currentWindowIndex
                        //Log.e(">>", "currentWindowIndex: $index")
                        val video = mVideos[index]
                        val html =
                            video.style + video.furigana + "<hr>" + video.korean + "<hr>" + video.japanese
                        webView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "")
                    }
                    Player.STATE_ENDED -> {
                        //Log.e(">>", "STATE_ENDED")
                        //val intent = Intent(BNS_ACTION_CONTENT_STOP)
                        //exoPlayerView.setPlayer(null)
                        //AudioPlayer.player.release() //플레이어 끝내기 (다른 곳에서 디코더를? 사용할 수 있게 해주기 위해 끝내줘야함)
                        //AudioPlayer.player = null
                        //mContext.sendBroadcast(intent) //서버에 끝났다고 알림
                        //if (getActivity() != null) {
                        //    getActivity().finish() //프레그먼트 없앰
                        //}
                    }
                    else -> {
                    }
                }
            }
        })

        playerView.player = exoPlayer
        playerView.controllerShowTimeoutMs = 0 // 터치 시 계속 보이기/숨기기
    }

    override fun onStart() {
        //Toast.makeText(this, "onStart(): $permissionGranted", Toast.LENGTH_LONG).show()
        super.onStart()
        checkPermissions()
    }

    override fun onStop() {
        super.onStop()
        if (playerView != null) playerView.onPause()
        releasePlayer()
    }

    private fun releasePlayer() {
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        exoPlayer.release()
    }

    /*
    override fun onSwipeLeft() {
        goArticles()
    }

    override fun onSwipeRight() {

    }
    */

    private fun goArticles() {
        val intent = Intent(this, ArticlesActivity::class.java)
        startActivityForResult(intent, Config.activityRequestCodeArticles)
    }
}
