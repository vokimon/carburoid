package net.canvoki.carburoid.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.net.toUri

inline fun <reified T : ComponentActivity> Context.openActivity(noinline configure: Intent.() -> Unit = {}): Boolean {
    val intent = Intent(this, T::class.java)
    intent.configure()
    startActivity(intent)
    return true
}

fun Context.openUri(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(intent)
}

fun Context.openUri(uri: String) {
    openUri(uri.toUri())
}
