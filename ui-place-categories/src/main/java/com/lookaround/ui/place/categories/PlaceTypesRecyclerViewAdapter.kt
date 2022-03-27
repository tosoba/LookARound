package com.lookaround.ui.place.categories

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lookaround.core.android.databinding.SpacerItemBinding
import com.lookaround.core.android.databinding.TransparentChipItemBinding
import com.lookaround.core.android.ext.setListBackgroundItemDrawableWith
import com.lookaround.core.android.view.recyclerview.DefaultDiffUtilCallback
import com.lookaround.core.android.view.recyclerview.ViewBindingViewHolder
import com.lookaround.core.model.IPlaceType
import com.lookaround.ui.place.categories.databinding.PlaceTypeItemBinding

internal class PlaceTypesRecyclerViewAdapter(
    private var items: List<PlaceTypeListItem>,
    private val onPlaceTypeClicked: (IPlaceType) -> Unit,
) : RecyclerView.Adapter<ViewBindingViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewBindingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewBindingViewHolder(
            when (ViewType.values()[viewType]) {
                ViewType.SPACER -> {
                    SpacerItemBinding.inflate(inflater, parent, false).apply {
                        root.layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                (items[0] as PlaceTypeListItem.Spacer).heightPx
                            )
                    }
                }
                ViewType.PLACE_TYPE -> {
                    PlaceTypeItemBinding.inflate(inflater, parent, false).apply {
                        root.setListBackgroundItemDrawableWith(Color.WHITE)
                    }
                }
                ViewType.PLACE_CATEGORY_HEADER -> {
                    TransparentChipItemBinding.inflate(inflater, parent, false).apply {
                        root.setListBackgroundItemDrawableWith(Color.WHITE)
                    }
                }
            }
        )
    }

    override fun onBindViewHolder(
        holder: ViewBindingViewHolder,
        position: Int
    ) {
        when (val binding = holder.binding) {
            is TransparentChipItemBinding -> bindPlaceCategoryHeaderItem(binding, position)
            is PlaceTypeItemBinding -> bindPlaceTypeItem(binding, position)
        }
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
