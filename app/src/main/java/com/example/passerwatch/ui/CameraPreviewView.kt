package com.example.passerwatch.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import java.util.Locale
import kotlin.math.roundToInt

/** Keep camera pixels inside the target, with the frosted badge outside it to avoid feedback. */
class CameraPreviewView(context: Context, private val preview: PreviewView) : FrameLayout(context) {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private val target = BlurTarget(context)
    private val badge = BlurView(context)
    private val label = TextView(context).apply {
        textSize = 14f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(8), dp(12), dp(8))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    private var lastDark: Boolean? = null

    init {
        clipChildren = true
        target.clipChildren = true
        target.addView(preview, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(target, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        badge.addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        badge.outlineProvider = ViewOutlineProvider.BACKGROUND
        badge.clipToOutline = true
        addView(badge, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply {
            setMargins(dp(14), dp(14), dp(14), dp(14))
        })
        // TextureView blur requires API 31+. Earlier devices keep a translucent readable badge.
        if (Build.VERSION.SDK_INT >= 31) badge.setupWith(target).setBlurRadius(14f)
    }

    fun releasePreview() { target.removeView(preview) }

    fun update(digitalCrop: Float, zoom: Float, dark: Boolean) {
        // Scale camera content only. The target, blur badge, and viewport remain fixed in place.
        preview.scaleX = digitalCrop
        preview.scaleY = digitalCrop
        val value = String.format(Locale.US, "%.1f×", zoom)
        if (label.text.toString() != value) {
            label.text = value
            label.contentDescription = "缩放 $value"
        }
        if (lastDark != dark) {
            lastDark = dark
            label.setTextColor(if (dark) Color.WHITE else Color.rgb(26, 41, 34))
            val tint = if (dark) 0x40202A25 else 0xA6F5FAF6.toInt()
            badge.background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (Build.VERSION.SDK_INT >= 31) Color.TRANSPARENT else tint)
                setStroke(dp(1), if (dark) 0x40FFFFFF else 0x66768C7E)
            }
            if (Build.VERSION.SDK_INT >= 31) badge.setOverlayColor(tint)
        }
    }
}
