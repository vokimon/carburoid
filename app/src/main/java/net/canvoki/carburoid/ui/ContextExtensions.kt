package net.canvoki.carburoid.ui

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity

inline fun <reified T : ComponentActivity> Context.openActivity(noinline configure: Intent.() -> Unit = {}): Boolean {
    val intent = Intent(this, T::class.java)
    intent.configure()
    startActivity(intent)
    return true
}
