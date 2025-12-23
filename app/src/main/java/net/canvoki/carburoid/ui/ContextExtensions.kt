package net.canvoki.carburoid.ui

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity

fun Context.openActivity(
    activityClass: Class<out ComponentActivity>,
    configure: Intent.() -> Unit = {},
): Boolean {
    val intent = Intent(this, activityClass)
    intent.configure()
    startActivity(intent)
    return true
}
