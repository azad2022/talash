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
        val bytesPerRow = (width + 7) / 8
        val imageBytes = ByteArray(bytesPerRow * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val luminance =
                    0.299 * ((pixel shr 16) and 0xFF) +
                    0.587 * ((pixel shr 8) and 0xFF) +
                    0.114 * (pixel and 0xFF)
                if (luminance < 128) {
                    val index = y * bytesPerRow + x / 8
                    imageBytes[index] = (imageBytes[index].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
        }

        return ByteArrayOutputStream().apply {
            write(INIT)
            write(byteArrayOf(0x1B, 0x61, 0x01))
            write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            write(bytesPerRow and 0xFF)
            write((bytesPerRow shr 8) and 0xFF)
            write(height and 0xFF)
            write((height shr 8) and 0xFF)
            write(imageBytes)
            write(byteArrayOf(0x0A, 0x0A, 0x0A))
            write(CUT)
        }.toByteArray()
    }}
