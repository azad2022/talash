package com.example.hardware.print

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

object EscPosEncoder {
    private val INIT = byteArrayOf(0x1B, 0x40)
    private val CUT = byteArrayOf(0x1D, 0x56, 0x00)

    fun encodeText(text: String, charset: java.nio.charset.Charset = Charsets.UTF_8): ByteArray =
        ByteArrayOutputStream().apply {
            write(INIT)
            write(byteArrayOf(0x1B, 0x61, 0x02))
            write(text.toByteArray(charset))
            write(byteArrayOf(0x0A, 0x0A, 0x0A))
            write(CUT)
        }.toByteArray()

    fun encodeRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = ceil(width / 8.0).toInt()
        val out = ByteArrayOutputStream()
        out.write(INIT)
        out.write(byteArrayOf(0x1B, 0x61, 0x01))

        for (y in 0 until height) {
            out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            out.write(bytesPerRow and 0xFF)
            out.write((bytesPerRow shr 8) and 0xFF)
            out.write(1)
            out.write(0)

            val row = ByteArray(bytesPerRow)
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                if (luminance < 128) {
                    row[x / 8] = (row[x / 8].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
            out.write(row)
        }
        out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
        out.write(CUT)
        return out.toByteArray()
    }
}
