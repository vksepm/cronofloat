package com.chronofloat.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "chronofloat_overlay"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.chronofloat.ACTION_STOP"
        const val ACTION_UPDATE_CONFIG = "com.chronofloat.ACTION_UPDATE_CONFIG"
        const val EXTRA_IS_24H = "is24h"
        const val EXTRA_SHOW_SECONDS = "showSeconds"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: TextView
    private lateinit var handler: Handler
    private var is24h = true
    private var showSeconds = false
    private var isRunning = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                updateTime()
                val interval = if (showSeconds) 1000L else 60000L
                handler.postDelayed(this, interval)
            }
        }
    }

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_CONFIG) {
                is24h = intent.getBooleanExtra(EXTRA_IS_24H, true)
                showSeconds = intent.getBooleanExtra(EXTRA_SHOW_SECONDS, false)
                updateTime()
                // Restart the timer with new interval
                handler.removeCallbacks(updateRunnable)
                handler.post(updateRunnable)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        is24h = intent?.getBooleanExtra(EXTRA_IS_24H, true) ?: true
        showSeconds = intent?.getBooleanExtra(EXTRA_SHOW_SECONDS, false) ?: false

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        createOverlayView()

        val filter = IntentFilter(ACTION_UPDATE_CONFIG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(configReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(configReceiver, filter)
        }

        isRunning = true
        handler.post(updateRunnable)

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        try {
            unregisterReceiver(configReceiver)
        } catch (_: Exception) {}
        try {
            windowManager.removeView(overlayView)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Clock Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the floating clock running"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val openPendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ChronoFloat")
            .setContentText("Clock overlay is active")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentIntent(openPendingIntent)
            .addAction(
                Notification.Action.Builder(
                    null, "Stop", stopPendingIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    private fun createOverlayView() {
        val dp = { value: Float ->
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
            ).toInt()
        }

        overlayView = TextView(this).apply {
            setTextColor(0xD9FFFFFF.toInt()) // White at 85% opacity
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
            setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)

            val bg = GradientDrawable().apply {
                setColor(0x66000000) // 40% black
                cornerRadius = dp(6f).toFloat()
            }
            background = bg
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8f)
            y = dp(32f)
        }

        setupDragListener(params)
        windowManager.addView(overlayView, params)
        updateTime()
    }

    private fun setupDragListener(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val dragThreshold = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics
        )

        overlayView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (Math.abs(dx) > dragThreshold || Math.abs(dy) > dragThreshold)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        // Since gravity is END, moving right means decreasing x
                        params.x = (initialX - dx.toInt())
                        params.y = (initialY + dy.toInt())
                        windowManager.updateViewLayout(overlayView, params)
                    }
                    isDragging
                }
                MotionEvent.ACTION_UP -> {
                    isDragging
                }
                else -> false
            }
        }
    }

    private fun updateTime() {
        val pattern = when {
            is24h && showSeconds -> "EEE, MMM d | HH:mm:ss"
            is24h && !showSeconds -> "EEE, MMM d | HH:mm"
            !is24h && showSeconds -> "EEE, MMM d | h:mm:ss a"
            else -> "EEE, MMM d | h:mm a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        overlayView.text = sdf.format(Date())
    }
}
