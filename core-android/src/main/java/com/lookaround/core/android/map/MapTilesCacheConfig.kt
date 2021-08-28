package com.lookaround.core.android.map

import android.content.Context
import com.lookaround.core.android.ext.getOrCreateCacheFile
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.CacheControl
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapTilesCacheConfig @Inject constructor(@ApplicationContext context: Context) {
    val cacheControl =
        CacheControl.Builder()
            .maxAge(TILE_CACHE_MAX_AGE_DAYS, TimeUnit.DAYS)
            .maxStale(TILE_CACHE_MAX_STALE_DAYS, TimeUnit.DAYS)
            .build()

    val cache: Cache? =
        context.getOrCreateCacheFile(TILE_CACHE_DIR)?.let { cacheDir ->
            if (cacheDir.exists()) {
                Cache(cacheDir, TILE_CACHE_SIZE_B)
            } else {
                Timber.tag("CACHE").d("Tile cache dir does not exist - not using cache.")
                null
            }
        }

    companion object {
        const val USER_AGENT_HEADER = "LookARound"

        private const val TILE_CACHE_DIR = "tile_cache"
        private const val TILE_CACHE_SIZE_B = 50L * 1024L * 1024L
        private const val TILE_CACHE_MAX_STALE_DAYS = 14
        private const val TILE_CACHE_MAX_AGE_DAYS = 1
    }
}
