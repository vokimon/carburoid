package net.canvoki.carburoid.ui

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding

private fun setupInsetListeners(content: View) {
    val initialLeft = content.paddingLeft
    val initialTop = content.paddingTop
    val initialRight = content.paddingRight
    val initialBottom = content.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
        val bars =
            insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    //WindowInsetsCompat.Type.systemGestures() or
                    0,
            )
        v.updatePadding(
            left = initialLeft + bars.left,
            top = initialTop + bars.top,
            right = initialRight + bars.right,
            bottom = initialBottom + bars.bottom,
        )
        WindowInsetsCompat.CONSUMED
    }
}

fun AppCompatActivity.setContentViewWithInsets(layoutId: Int) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    setContentView(layoutId)
    val content = findViewById<View>(android.R.id.content)
    setupInsetListeners(content)
}

fun <T : ViewBinding> AppCompatActivity.setContentViewWithInsets(inflate: (LayoutInflater) -> T): T {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val binding = inflate(layoutInflater)
    setContentView(binding.root)
    setupInsetListeners(binding.root)
    return binding
}
