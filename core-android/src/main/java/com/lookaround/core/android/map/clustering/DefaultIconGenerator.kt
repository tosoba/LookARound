package com.lookaround.core.android.map.clustering

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.SparseArray
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.lookaround.core.android.R

class DefaultIconGenerator<T : ClusterItem>(
    private val context: Context,
    private val iconStyle: IconStyle = getCreateDefaultIconStyle(context)
) : IconGenerator<T> {
    private val clusterItemIcon: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, iconStyle.clusterIconResId)
    }
    private val clusterIcons = SparseArray<Bitmap>()

    private val clusterBackground: Drawable
        get() =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(iconStyle.clusterBackgroundColor)
                setStroke(iconStyle.clusterStrokeWidth, iconStyle.clusterStrokeColor)
            }

    override fun getClusterIcon(cluster: Cluster<T>): Bitmap {
        val clusterBucket = getClusterIconBucket(cluster)
        var clusterIcon = clusterIcons[clusterBucket]
        if (clusterIcon == null) {
            clusterIcon = createClusterIcon(clusterBucket)
            clusterIcons.put(clusterBucket, clusterIcon)
        }
        return clusterIcon
    }

    override fun getClusterItemIcon(clusterItem: T): Bitmap = clusterItemIcon

    private fun createClusterIcon(clusterBucket: Int): Bitmap {
        @SuppressLint("InflateParams")
        val clusterIconView =
            LayoutInflater.from(context).inflate(R.layout.map_cluster_icon, null) as TextView
        clusterIconView.background = clusterBackground
        clusterIconView.setTextColor(iconStyle.clusterTextColor)
        clusterIconView.setTextSize(TypedValue.COMPLEX_UNIT_PX, iconStyle.clusterTextSize.toFloat())
        clusterIconView.text = getClusterIconText(clusterBucket)
        clusterIconView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        clusterIconView.layout(0, 0, clusterIconView.measuredWidth, clusterIconView.measuredHeight)
        val iconBitmap =
            Bitmap.createBitmap(
                clusterIconView.measuredWidth,
                clusterIconView.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
        val canvas = Canvas(iconBitmap)
        clusterIconView.draw(canvas)
        return iconBitmap
    }

    private fun getClusterIconBucket(cluster: Cluster<T>): Int {
        val itemCount = cluster.items.size
        if (itemCount <= CLUSTER_ICON_BUCKETS[0]) return itemCount
        for (i in 0 until CLUSTER_ICON_BUCKETS.size - 1) {
            if (itemCount < CLUSTER_ICON_BUCKETS[i + 1]) return CLUSTER_ICON_BUCKETS[i]
        }
        return CLUSTER_ICON_BUCKETS[CLUSTER_ICON_BUCKETS.size - 1]
    }

    private fun getClusterIconText(clusterIconBucket: Int): String =
        if (clusterIconBucket < CLUSTER_ICON_BUCKETS[0]) clusterIconBucket.toString()
        else "$clusterIconBucket+"

    companion object {
        private val CLUSTER_ICON_BUCKETS =
            intArrayOf(10, 20, 50, 100, 500, 1000, 5000, 10000, 20000)

        private fun getCreateDefaultIconStyle(context: Context): IconStyle =
            IconStyle.Builder(context).build()
    }
}
