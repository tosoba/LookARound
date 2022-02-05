package com.lookaround.core.android.ext

import android.annotation.SuppressLint
import android.graphics.*
import android.media.Image
import androidx.camera.core.ImageProxy
import androidx.palette.graphics.Palette
import java.io.ByteArrayOutputStream
import java.nio.ReadOnlyBufferException
import kotlin.experimental.inv

val ImageProxy.bitmap: Bitmap?
    @SuppressLint("UnsafeOptInUsageError")
    get() = use {
        it.image?.yuv420888ToNv21?.let { byteArray ->
            nv21BytesToBitmap(byteArray, width, height, 90f)
        }
    }

private val Image.yuv420888ToNv21: ByteArray
    get() {
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V
        var rowStride = planes[0].rowStride
        assert(planes[0].pixelStride == 1)
        var pos = 0
        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize)
            pos += ySize
        } else {
            var yBufferPos = -rowStride.toLong() // not an actual position
            while (pos < ySize) {
                yBufferPos += rowStride.toLong()
                yBuffer.position(yBufferPos.toInt())
                yBuffer.get(nv21, pos, width)
                pos += width
            }
        }
        rowStride = planes[2].rowStride
        val pixelStride = planes[2].pixelStride
        assert(rowStride == planes[1].rowStride)
        assert(pixelStride == planes[1].pixelStride)
        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of
            // uBuffer[0]
            val savePixel: Byte = vBuffer.get(1)
            try {
                vBuffer.put(1, savePixel.inv())
                if (uBuffer.get(0) == savePixel.inv()) {
                    vBuffer.put(1, savePixel)
                    vBuffer.position(0)
                    uBuffer.position(0)
                    vBuffer.get(nv21, ySize, 1)
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining())
                    return nv21 // shortcut
                }
            } catch (ex: ReadOnlyBufferException) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel)
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = vBuffer.get(vuPos)
                nv21[pos++] = uBuffer.get(vuPos)
            }
        }
        return nv21
    }

private fun nv21BytesToBitmap(
    byteArray: ByteArray,
    width: Int,
    height: Int,
    rotationDegrees: Float = 0f
): Bitmap? {
    val yuvImage = YuvImage(byteArray, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 50, out)
    val imageBytes = out.toByteArray()
    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    if (rotationDegrees != 0f) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
        bitmap =
            Bitmap.createBitmap(
                scaledBitmap,
                0,
                0,
                scaledBitmap.width,
                scaledBitmap.height,
                matrix,
                true
            )
    }
    return bitmap
}

val Bitmap.dominantSwatch: Palette.Swatch?
    get() = Palette.from(this).generate().dominantSwatch
