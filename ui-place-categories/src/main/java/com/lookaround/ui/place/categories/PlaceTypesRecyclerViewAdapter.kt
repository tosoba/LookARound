package com.lookaround.ui.place.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lookaround.core.android.databinding.TransparentChipItemBinding
import com.lookaround.core.android.ext.setListBackgroundItemDrawableWith
import com.lookaround.core.android.view.recyclerview.ColorRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.recyclerview.DefaultDiffUtilCallback
import com.lookaround.core.android.view.recyclerview.TransparentChipsRecyclerViewAdapter
import com.lookaround.core.model.IPlaceType
import com.lookaround.ui.place.categories.databinding.PlaceTypeItemBinding
import com.lookaround.ui.place.categories.databinding.TopSpacerItemBinding

internal class PlaceTypesRecyclerViewAdapter(
    private var items: List<PlaceTypeListItem>,
    private val colorCallbacks: ColorRecyclerViewAdapterCallbacks,
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
        colorCallbacks.onDetachedFromRecyclerView()
    }

    override fun onViewAttachedToWindow(holder: TransparentChipsRecyclerViewAdapter.ViewHolder) {
        if (holder.binding is TopSpacerItemBinding) return
        colorCallbacks.onViewAttachedToWindow(holder.uuid) { contrastingColor ->
            holder.binding.root.setListBackgroundItemDrawableWith(contrastingColor)
        }
    }

    override fun onViewDetachedFromWindow(holder: TransparentChipsRecyclerViewAdapter.ViewHolder) {
        if (holder.binding is TopSpacerItemBinding) return
        colorCallbacks.onViewDetachedFromWindow(holder.uuid)
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
