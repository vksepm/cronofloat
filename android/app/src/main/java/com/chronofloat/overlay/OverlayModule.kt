package com.chronofloat.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import androidx.core.content.ContextCompat

class OverlayModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "OverlayModule"

    @ReactMethod
    fun checkPermission(promise: Promise) {
        promise.resolve(Settings.canDrawOverlays(reactContext))
    }

    @ReactMethod
    fun requestPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${reactContext.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        reactContext.startActivity(intent)
    }

    @ReactMethod
    fun startOverlay(config: ReadableMap) {
        if (!Settings.canDrawOverlays(reactContext)) {
            return
        }

        val intent = Intent(reactContext, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_IS_24H, config.getBoolean("is24h"))
            putExtra(OverlayService.EXTRA_SHOW_SECONDS, config.getBoolean("showSeconds"))
        }
        ContextCompat.startForegroundService(reactContext, intent)
    }

    @ReactMethod
    fun stopOverlay() {
        val intent = Intent(reactContext, OverlayService::class.java)
        reactContext.stopService(intent)
    }

    @ReactMethod
    fun updateConfig(config: ReadableMap) {
        val intent = Intent(OverlayService.ACTION_UPDATE_CONFIG).apply {
            setPackage(reactContext.packageName)
            putExtra(OverlayService.EXTRA_IS_24H, config.getBoolean("is24h"))
            putExtra(OverlayService.EXTRA_SHOW_SECONDS, config.getBoolean("showSeconds"))
        }
        reactContext.sendBroadcast(intent)
    }
}
