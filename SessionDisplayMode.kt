package com.nebula.app.features.streaming

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Switches to immersive landscape while a real stream is active and restores portrait
 * afterwards. In this phase no stream can start, so this is never enabled.
 */
@Composable
fun SessionDisplayMode(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(enabled, activity) {
        if (!enabled || activity == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
