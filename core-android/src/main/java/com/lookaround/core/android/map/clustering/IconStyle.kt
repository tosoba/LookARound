package com.lookaround.core.android.map.clustering

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.lookaround.core.android.R

class IconStyle private constructor(builder: Builder) {
    @get:ColorInt val clusterBackgroundColor: Int
    @get:ColorInt val clusterTextColor: Int
    @get:ColorInt val clusterStrokeColor: Int
    val clusterStrokeWidth: Int
    val clusterTextSize: Int
    @get:DrawableRes val clusterIconResId: Int

    init {
        clusterBackgroundColor = builder.clusterBackgroundColor
        clusterTextColor = builder.clusterTextColor
        clusterStrokeColor = builder.clusterStrokeColor
        clusterStrokeWidth = builder.clusterStrokeWidth
        clusterTextSize = builder.clusterTextSize
        clusterIconResId = builder.clusterIconResId
    }

    class Builder(context: Context) {
        var clusterBackgroundColor: Int
        var clusterTextColor: Int
        var clusterStrokeColor: Int
        var clusterStrokeWidth: Int
        var clusterTextSize: Int
        var clusterIconResId: Int

        init {
            clusterBackgroundColor = ContextCompat.getColor(context, R.color.cluster_background)
            clusterTextColor = ContextCompat.getColor(context, R.color.cluster_text)
            clusterStrokeColor = ContextCompat.getColor(context, R.color.cluster_stroke)
            clusterStrokeWidth =
                context.resources.getDimensionPixelSize(R.dimen.cluster_stroke_width)
            clusterTextSize = context.resources.getDimensionPixelSize(R.dimen.cluster_text_size)
            clusterIconResId = R.drawable.ic_map_marker
        }

        fun setClusterBackgroundColor(@ColorInt color: Int): Builder {
            clusterBackgroundColor = color
            return this
        }

        fun setClusterTextColor(@ColorInt color: Int): Builder {
            clusterTextColor = color
            return this
        }

        fun setClusterStrokeColor(@ColorInt color: Int): Builder {
            clusterStrokeColor = color
            return this
        }

        fun setClusterStrokeWidth(width: Int): Builder {
            clusterStrokeWidth = width
            return this
        }

        fun setClusterTextSize(size: Int): Builder {
            clusterTextSize = size
            return this
        }

        fun setClusterIconResId(@DrawableRes resId: Int): Builder {
            clusterIconResId = resId
            return this
        }

        fun build(): IconStyle = IconStyle(this)
    }
}
