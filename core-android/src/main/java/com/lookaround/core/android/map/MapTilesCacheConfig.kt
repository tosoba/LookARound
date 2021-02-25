package com.lookaround.core.android.map

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.CacheControl
import java.io.File
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

    val cache: Cache?

    init {
        val cacheDir = context.externalCacheDir
        val tileCacheDir: File?
        if (cacheDir != null) {
            tileCacheDir = File(cacheDir, TILE_CACHE_DIR)
            if (!tileCacheDir.exists()) tileCacheDir.mkdir()
        } else {
            tileCacheDir = null
        }
        cache = if (tileCacheDir?.exists() == true) Cache(tileCacheDir, TILE_CACHE_SIZE_B) else null
    }

    companion object {
        const val USER_AGENT_HEADER = "LookARound"

        private const val TILE_CACHE_DIR = "tile_cache"
        private const val TILE_CACHE_SIZE_B = 50L * 1024L * 1024L
        private const val TILE_CACHE_MAX_STALE_DAYS = 14
        private const val TILE_CACHE_MAX_AGE_DAYS = 1
    }
}
