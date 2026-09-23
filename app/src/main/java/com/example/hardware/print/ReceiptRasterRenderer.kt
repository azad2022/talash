package com.example.hardware.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.graphics.Typeface

object ReceiptRasterRenderer {
    fun render(
        text: String,
        widthPx: Int = 576,
        textSizePx: Float = 25f,
        paddingPx: Int = 18
    ): Bitmap {
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSizePx
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val contentWidth = (widthPx - paddingPx * 2).coerceAtLeast(1)
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(true)
            .build()

        val bitmap = Bitmap.createBitmap(
            widthPx,
            layout.height + paddingPx * 2,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.translate(paddingPx.toFloat(), paddingPx.toFloat())
        layout.draw(canvas)
        canvas.restore()
        return bitmap
    }
}
