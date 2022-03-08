package com.lookaround.ui.place.categories

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lookaround.core.android.R
import com.lookaround.core.android.databinding.TransparentChipItemBinding
import com.lookaround.core.android.view.recyclerview.ColorRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.recyclerview.DefaultDiffUtilCallback
import com.lookaround.core.android.view.recyclerview.TransparentChipsRecyclerViewAdapter
import com.lookaround.core.model.IPlaceType
import com.lookaround.ui.place.categories.databinding.PlaceTypeItemBinding
import com.lookaround.ui.place.categories.databinding.TopSpacerItemBinding

internal class PlaceTypesRecyclerViewAdapter(
    private var items: List<PlaceTypeListItem>,
    private val callbacks: ColorRecyclerViewAdapterCallbacks,
    private val onPlaceTypeClicked: (IPlaceType) -> Unit,
) : RecyclerView.Adapter<TransparentChipsRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransparentChipsRecyclerViewAdapter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return TransparentChipsRecyclerViewAdapter.ViewHolder(
            when (ViewType.values()[viewType]) {
                ViewType.SPACER ->
                    TopSpacerItemBinding.inflate(inflater, parent, false).apply {
                        root.layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                (items[0] as PlaceTypeListItem.Spacer).heightPx
                            )
                    }
                ViewType.PLACE_TYPE -> PlaceTypeItemBinding.inflate(inflater, parent, false)
                ViewType.PLACE_CATEGORY_HEADER ->
                    TransparentChipItemBinding.inflate(inflater, parent, false)
            }
        )
    }

    override fun onBindViewHolder(
        holder: TransparentChipsRecyclerViewAdapter.ViewHolder,
        position: Int
    ) {
        when (val binding = holder.binding) {
            is TransparentChipItemBinding -> bindPlaceCategoryHeaderItem(binding, position)
            is PlaceTypeItemBinding -> bindPlaceTypeItem(binding, position)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        callbacks.onDetachedFromRecyclerView()
    }

    override fun onViewAttachedToWindow(holder: TransparentChipsRecyclerViewAdapter.ViewHolder) {
        if (holder.binding is TopSpacerItemBinding) return
        callbacks.onViewAttachedToWindow(holder.uuid) { contrastingColor ->
            val backgroundDrawable =
                ResourcesCompat.getDrawable(
                    holder.binding.root.resources,
                    R.drawable.rounded_elevated_background,
                    null
                ) as
                    LayerDrawable
            val backgroundLayer =
                backgroundDrawable.findDrawableByLayerId(
                    R.id.rounded_transparent_shadow_background_layer
                ) as
                    GradientDrawable
            backgroundLayer.color =
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(contrastingColor, 0x30))
            holder.binding.root.background = backgroundDrawable
        }
    }

    override fun onViewDetachedFromWindow(holder: TransparentChipsRecyclerViewAdapter.ViewHolder) {
        if (holder.binding is TopSpacerItemBinding) return
        callbacks.onViewDetachedFromWindow(holder.uuid)
    }

    private fun bindPlaceCategoryHeaderItem(binding: TransparentChipItemBinding, position: Int) {
        val item = items[position] as PlaceTypeListItem.PlaceCategory
        binding.chipLabelTextView.text = item.name
    }

    private fun bindPlaceTypeItem(binding: PlaceTypeItemBinding, position: Int) {
        val item = items[position] as PlaceTypeListItem.PlaceType<*>
        binding.placeTypeText.text = item.wrapped.label
        Glide.with(binding.root).load(item.drawableId).into(binding.placeTypeImage)
        binding.root.setOnClickListener { onPlaceTypeClicked(item.wrapped) }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is PlaceTypeListItem.Spacer -> ViewType.SPACER.ordinal
            is PlaceTypeListItem.PlaceCategory -> ViewType.PLACE_CATEGORY_HEADER.ordinal
            is PlaceTypeListItem.PlaceType<*> -> ViewType.PLACE_TYPE.ordinal
        }

    fun updateItems(newItems: List<PlaceTypeListItem>) {
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    private enum class ViewType {
        SPACER,
        PLACE_TYPE,
        PLACE_CATEGORY_HEADER
    }
}
