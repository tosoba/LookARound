package com.lookaround.ui.place.map.list

import android.graphics.Bitmap
import android.location.Location
import uk.co.senab.bitmapcache.CacheableBitmapDrawable
import java.lang.ref.WeakReference

internal data class MapCaptureRequest(
    val location: Location,
    val bitmapCallback: (Bitmap) -> Unit,
    val cacheableBitmapDrawableCallback: (CacheableBitmapDrawable) -> Unit
) {
    constructor(
        location: Location,
        holder: WeakReference<PlaceMapListViewHolder>
    ) : this(
        location,
        bitmapCallback = { bitmap -> holder.get()?.placeMapImageView?.setImageBitmap(bitmap) },
        cacheableBitmapDrawableCallback = { drawable ->
            holder.get()?.placeMapImageView?.setImageDrawable(drawable)
        }
    )
}