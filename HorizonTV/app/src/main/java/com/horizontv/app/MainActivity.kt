package com.horizontv.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.json.JSONObject

private data class TvChannel(val number: Int, val name: String, val url: String, val category: String, val tvgId: String)

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val splashDuration = 15_000L
    private lateinit var root: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var infoPanel: LinearLayout
    private lateinit var channelText: TextView
    private lateinit var statusText: TextView
    private lateinit var splashProgress: ProgressBar
    private var player: ExoPlayer? = null
    private var channels: List<TvChannel> = emptyList()
    private var currentIndex = 0
    private var overlayVisible = true
    private var overlayRunnable: Runnable? = null
    private var splashStartedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        channels = loadChannels()
        showSplash()
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun loadChannels(): List<TvChannel> {
        return try {
            val json = assets.open("channels.json").bufferedReader().use { it.readText() }
            val array = JSONObject(json).getJSONArray("channels")
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                TvChannel(item.getInt("number"), item.getString("name"), item.getString("url"),
                    item.optString("category", "Televisión"), item.optString("tvg_id", ""))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun showSplash() {
        root = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(3, 13, 30), Color.rgb(8, 38, 72), Color.rgb(2, 12, 27)))
        }
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 24, 32, 24)
        }
        val logo = TextView(this).apply {
            text = "HorizonTV"
            textSize = 42f
            letterSpacing = 0.08f
            setTextColor(Color.rgb(235, 246, 255))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "TELEVISIÓN SIN COMPLICACIONES"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(Color.rgb(128, 183, 226))
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 36)
        }
        splashProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(65, 177, 255))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(34, 65, 95))
        }
        val progressParams = LinearLayout.LayoutParams((resources.displayMetrics.widthPixels * 0.55f).toInt(), 8)
        val loading = TextView(this).apply {
            text = "Preparando tu televisión..."
            textSize = 13f
            setTextColor(Color.rgb(174, 205, 232))
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 0)
        }
        center.addView(logo)
        center.addView(subtitle)
        center.addView(splashProgress, progressParams)
        center.addView(loading)
        root.addView(center, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        splashStartedAt = System.currentTimeMillis()
        val progressTick = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - splashStartedAt
                splashProgress.progress = (elapsed * 100 / splashDuration).toInt().coerceIn(0, 100)
                if (elapsed < splashDuration) handler.postDelayed(this, 100)
            }
        }
        handler.post(progressTick)
        handler.postDelayed({ startTelevision() }, splashDuration)
    }

    private fun startTelevision() {
        if (channels.isEmpty()) {
            showFatalMessage("No se pudo cargar la parrilla de canales.")
            return
        }
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        playerView = PlayerView(this).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShutterBackgroundColor(Color.BLACK)
        }
        frame.addView(playerView, FrameLayout.LayoutParams(-1, -1))

        infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(26, 16, 26, 16)
            background = GradientDrawable().apply {
                colors = intArrayOf(0xE8102036.toInt(), 0xD9081B30.toInt())
                cornerRadius = 18f
            }
        }
        channelText = TextView(this).apply {
            textSize = 23f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        statusText = TextView(this).apply {
            text = "${channels[currentIndex].category}"
            textSize = 12f
            setTextColor(Color.rgb(174, 205, 232))
            setPadding(0, 5, 0, 9)
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val previousButton = TextView(this).apply {
            text = "◀  CH−  ANTERIOR"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(18, 13, 18, 13)
            background = GradientDrawable().apply { setColor(0xFF17436B.toInt()); cornerRadius = 12f }
            setOnClickListener { previousChannel() }
        }
        val nextButton = TextView(this).apply {
            text = "SIGUIENTE  CH+  ▶"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(18, 13, 18, 13)
            background = GradientDrawable().apply { setColor(0xFF176AA3.toInt()); cornerRadius = 12f }
            setOnClickListener { nextChannel() }
        }
        controls.addView(previousButton, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 8 })
        controls.addView(nextButton, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = 8 })
        infoPanel.addView(channelText)
        infoPanel.addView(statusText)
        infoPanel.addView(controls)
        val panelParams = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            leftMargin = 24; rightMargin = 24; bottomMargin = 28
        }
        frame.addView(infoPanel, panelParams)
        frame.setOnClickListener { toggleOverlay() }
        setContentView(frame)

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    statusText.text = "Señal no disponible · intentando recuperar..."
                    handler.postDelayed({ playCurrentChannel() }, 4_000)
                }
            })
        }
        currentIndex = channels.indexOfFirst { it.number == 1 }.takeIf { it >= 0 } ?: 0
        playCurrentChannel()
        showOverlayTemporarily()
    }

    private fun playCurrentChannel() {
        if (channels.isEmpty()) return
        val channel = channels[currentIndex]
        channelText.text = String.format("%03d  ·  %s", channel.number, channel.name)
        statusText.text = "${channel.category}"
        player?.apply {
            stop()
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
            playWhenReady = true
        }
        showOverlayTemporarily()
    }

    private fun nextChannel() {
        if (channels.isEmpty()) return
        currentIndex = (currentIndex + 1) % channels.size
        playCurrentChannel()
    }

    private fun previousChannel() {
        if (channels.isEmpty()) return
        currentIndex = if (currentIndex == 0) channels.lastIndex else currentIndex - 1
        playCurrentChannel()
    }

    private fun toggleOverlay() {
        overlayVisible = !overlayVisible
        infoPanel.visibility = if (overlayVisible) View.VISIBLE else View.GONE
        if (overlayVisible) showOverlayTemporarily()
    }

    private fun showOverlayTemporarily() {
        overlayVisible = true
        infoPanel.visibility = View.VISIBLE
        overlayRunnable?.let { handler.removeCallbacks(it) }
        overlayRunnable = Runnable {
            overlayVisible = false
            if (::infoPanel.isInitialized) infoPanel.visibility = View.GONE
        }
        handler.postDelayed(overlayRunnable!!, 5_000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PAGE_DOWN -> { nextChannel(); return true }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_PAGE_UP -> { previousChannel(); return true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { toggleOverlay(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showFatalMessage(message: String) {
        val text = TextView(this).apply {
            this.text = message
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(5, 20, 40))
        }
        setContentView(text)
    }

    override fun onStart() {
        super.onStart()
        player?.play()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }
}
