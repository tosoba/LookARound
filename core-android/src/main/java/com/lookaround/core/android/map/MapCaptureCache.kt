package com.lookaround.core.android.map

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import com.lookaround.core.android.ext.getOrCreateCacheFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber
import uk.co.senab.bitmapcache.BitmapLruCache
import uk.co.senab.bitmapcache.CacheableBitmapDrawable

@Singleton
class MapCaptureCache @Inject constructor(@ApplicationContext private val context: Context) {
    private var cache: BitmapLruCache? = initCache(context)

    val isEnabled: Boolean
        get() = cache != null

    private val Location.url: String
        get() = "${latitude}${longitude}"

    operator fun get(location: Location): CacheableBitmapDrawable? = cache?.get(location.url)
    operator fun set(location: Location, bitmap: Bitmap): CacheableBitmapDrawable? =
        cache?.put(location.url, bitmap)

    @Synchronized
    fun clear() {
        if (cache != null) {
            File(BITMAP_CACHE_DIR).deleteRecursively()
            cache = initCache(context)
        } else {
            Timber.tag(LOG_TAG).d("Bitmap cache does not exist - unable to clear.")
        }
    }

    private fun initCache(context: Context): BitmapLruCache? =
        context.getOrCreateCacheFile(BITMAP_CACHE_DIR)?.let { cacheDir ->
            if (cacheDir.exists()) {
                BitmapLruCache.Builder(context)
                    .setMemoryCacheEnabled(true)
                    .setMemoryCacheMaxSizeUsingHeapSize()
                    .setDiskCacheEnabled(true)
                    .setDiskCacheLocation(cacheDir)
                    .build()
            } else {
                Timber.tag(LOG_TAG).d("Bitmap cache dir does not exist - not using cache.")
                null
            }
        }

    companion object {
        private const val BITMAP_CACHE_DIR = "bitmap_cache"
        private const val LOG_TAG = "CACHE"
    }
}
