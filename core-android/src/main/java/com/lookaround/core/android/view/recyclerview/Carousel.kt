package com.lookaround.core.android.view.recyclerview

import android.content.Context
import android.graphics.Rect
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.Px
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.lookaround.core.android.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.parcelize.Parcelize

/** Works best with a [LinearLayoutManager] in [LinearLayoutManager.HORIZONTAL] orientation */
class LinearHorizontalSpacingDecoration(@Px private val innerSpacing: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)

        outRect.left = if (itemPosition == 0) 0 else innerSpacing / 2
        outRect.right = if (itemPosition == state.itemCount - 1) 0 else innerSpacing / 2
    }
}

/** Offset the first and last items to center them */
class BoundsOffsetDecoration : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)

        // It is crucial to refer to layoutParams.width (view.width is 0 at this time)!
        val itemWidth = view.layoutParams.width
        val offset = (parent.width - itemWidth) / 2

        if (itemPosition == 0) {
            outRect.left = offset
        } else if (itemPosition == state.itemCount - 1) {
            outRect.right = offset
        }
    }
}

@Parcelize
data class Image(val url: String, val width: Int, val height: Int) : Parcelable {
    val aspectRatio: Float
        get() = width.toFloat() / height.toFloat()
}

open class CarouselAdapter(private val images: List<Image>) :
    RecyclerView.Adapter<CarouselAdapter.VH>() {

    private var hasInitParentDimensions = false
    private var maxImageWidth: Int = 0
    private var maxImageHeight: Int = 0
    private var maxImageAspectRatio: Float = 1f

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // At this point [parent] has been measured and has valid width & height
        if (!hasInitParentDimensions) {
            maxImageWidth =
                parent.width - 2 * parent.resources.getDimensionPixelSize(R.dimen.gradient_width)
            maxImageHeight = parent.height
            maxImageAspectRatio = maxImageWidth.toFloat() / maxImageHeight.toFloat()
            hasInitParentDimensions = true
        }

        return VH(ImageView(parent.context))
    }

    override fun onBindViewHolder(vh: VH, position: Int) {
        val image = images[position]

        // Change aspect ratio
        val imageAspectRatio = image.aspectRatio
        val targetImageWidth: Int =
            if (imageAspectRatio < maxImageAspectRatio) {
                // Tall image: height = max
                (maxImageHeight * imageAspectRatio).roundToInt()
            } else {
                // Wide image: width = max
                maxImageWidth
            }
        vh.overlayableImageView.layoutParams =
            RecyclerView.LayoutParams(targetImageWidth, RecyclerView.LayoutParams.MATCH_PARENT)
    }

    override fun getItemCount(): Int = images.size

    class VH(val overlayableImageView: ImageView) : RecyclerView.ViewHolder(overlayableImageView)
}

private fun RecyclerView.smoothScrollToCenteredPosition(position: Int) {
    val smoothScroller =
        object : LinearSmoothScroller(context) {
            override fun calculateDxToMakeVisible(view: View?, snapPreference: Int): Int {
                val dxToStart = super.calculateDxToMakeVisible(view, SNAP_TO_START)
                val dxToEnd = super.calculateDxToMakeVisible(view, SNAP_TO_END)

                return (dxToStart + dxToEnd) / 2
            }
        }

    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}

/**
 * Arranges items so that the central one appears prominent: its neighbors are scaled down. Based on
 * https://stackoverflow.com/a/54516315/2291104
 */
internal class ProminentLayoutManager(
    context: Context,

    /**
     * This value determines where items reach the final (minimum) scale:
     * - 1f is when their center is at the start/end of the RecyclerView
     * - <1f is before their center reaches the start/end of the RecyclerView
     * - >1f is outside the bounds of the RecyclerView
     */
    private val minScaleDistanceFactor: Float = 1.5f,

    /** The final (minimum) scale for non-prominent items is 1-[scaleDownBy] */
    private val scaleDownBy: Float = 0.5f
) : LinearLayoutManager(context, HORIZONTAL, false) {

    private val prominentThreshold =
        context.resources.getDimensionPixelSize(R.dimen.prominent_threshold)

    override fun onLayoutCompleted(state: RecyclerView.State?) =
        super.onLayoutCompleted(state).also { scaleChildren() }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ) =
        super.scrollHorizontallyBy(dx, recycler, state).also {
            if (orientation == HORIZONTAL) scaleChildren()
        }

    private fun scaleChildren() {
        val containerCenter = width / 2f

        // Any view further than this threshold will be fully scaled down
        val scaleDistanceThreshold = minScaleDistanceFactor * containerCenter

        var translationXForward = 0f

        for (i in 0 until childCount) {
            val child = getChildAt(i)!!

            val childCenter = (child.left + child.right) / 2f
            val distanceToCenter = abs(childCenter - containerCenter)

            child.isActivated = distanceToCenter < prominentThreshold

            val scaleDownAmount = (distanceToCenter / scaleDistanceThreshold).coerceAtMost(1f)
            val scale = 1f - scaleDownBy * scaleDownAmount

            child.scaleX = scale
            child.scaleY = scale

            val translationDirection = if (childCenter > containerCenter) -1 else 1
            val translationXFromScale = translationDirection * child.width * (1 - scale) / 2f
            child.translationX = translationXFromScale + translationXForward

            translationXForward = 0f

            if (translationXFromScale > 0 && i >= 1) {
                // Edit previous child
                getChildAt(i - 1)!!.translationX += 2 * translationXFromScale
            } else if (translationXFromScale < 0) {
                // Pass on to next child
                translationXForward = 2 * translationXFromScale
            }
        }
    }

    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        // Since we're scaling down items, we need to pre-load more of them offscreen.
        // The value is sort of empirical: the more we scale down, the more extra space we need.
        return (width / (1 - scaleDownBy)).roundToInt()
    }
}
