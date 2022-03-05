package com.lookaround.ui.place.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.lookaround.core.model.IPlaceType
import com.lookaround.ui.place.categories.databinding.PlaceCategoryHeaderItemBinding
import com.lookaround.ui.place.categories.databinding.PlaceTypeItemBinding
import com.lookaround.ui.place.categories.databinding.TopSpacerItemBinding

internal class PlaceTypesRecyclerViewAdapter(
    private val placeTypeListItems: List<PlaceTypeListItem>,
    private val onPlaceTypeClicked: (IPlaceType) -> Unit,
) : RecyclerView.Adapter<PlaceTypesRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            when (ViewType.values()[viewType]) {
                ViewType.SPACER ->
                    TopSpacerItemBinding.inflate(inflater, parent, false).apply {
                        root.layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                (placeTypeListItems[0] as PlaceTypeListItem.Spacer).heightPx
                            )
                    }
                ViewType.PLACE_TYPE -> PlaceTypeItemBinding.inflate(inflater, parent, false)
                ViewType.PLACE_CATEGORY_HEADER ->
                    PlaceCategoryHeaderItemBinding.inflate(inflater, parent, false)
            }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val binding = holder.binding) {
            is PlaceCategoryHeaderItemBinding -> bindPlaceCategoryHeaderItem(binding, position)
            is PlaceTypeItemBinding -> bindPlaceTypeItem(binding, position)
        }
    }

    private fun bindPlaceCategoryHeaderItem(
        binding: PlaceCategoryHeaderItemBinding,
        position: Int
    ) {
        val item = placeTypeListItems[position] as PlaceTypeListItem.PlaceCategory
        binding.placeCategoryNameChip.text = item.name
    }

    private fun bindPlaceTypeItem(binding: PlaceTypeItemBinding, position: Int) {
        val item = placeTypeListItems[position] as PlaceTypeListItem.PlaceType
        binding.placeTypeText.text = item.wrapped.label
        Glide.with(binding.root).load(item.drawableId).into(binding.placeTypeImage)
        binding.root.setOnClickListener { onPlaceTypeClicked(item.wrapped) }
    }

    override fun getItemCount(): Int = placeTypeListItems.size

    override fun getItemViewType(position: Int): Int =
        when (placeTypeListItems[position]) {
            is PlaceTypeListItem.Spacer -> ViewType.SPACER.ordinal
            is PlaceTypeListItem.PlaceCategory -> ViewType.PLACE_CATEGORY_HEADER.ordinal
            is PlaceTypeListItem.PlaceType -> ViewType.PLACE_TYPE.ordinal
        }

    internal class ViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    private enum class ViewType {
        SPACER,
        PLACE_TYPE,
        PLACE_CATEGORY_HEADER
    }
}
