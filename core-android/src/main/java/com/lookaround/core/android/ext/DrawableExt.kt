package com.lookaround.core.android.ext

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

fun Drawable.asBitmapDrawable(
    resources: Resources,
    width: Int = intrinsicWidth,
    height: Int = intrinsicHeight
): BitmapDrawable =
    if (this is BitmapDrawable) this else BitmapDrawable(resources, createBitmap(width, height))

fun Drawable.createBitmap(width: Int = intrinsicWidth, height: Int = intrinsicHeight): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
