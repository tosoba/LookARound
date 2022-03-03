package com.lookaround.ui.place.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.lookaround.core.model.IPlaceType
import com.lookaround.ui.place.categories.model.PlaceTypeListItem

internal class PlaceTypesRecyclerViewAdapter(
    private val placeTypeListItems: List<PlaceTypeListItem>,
    private val onPlaceTypeClicked: (IPlaceType) -> Unit,
) : RecyclerView.Adapter<PlaceTypesRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (ViewType.values()[viewType]) {
            ViewType.PLACE_TYPE ->
                ViewHolder.PlaceTypeItemViewHolder(parent, R.layout.place_type_item)
            ViewType.PLACE_CATEGORY_HEADER ->
                ViewHolder.PlaceCategoryHeaderItemViewHolder(
                    parent,
                    R.layout.place_category_header_item
                )
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.PlaceCategoryHeaderItemViewHolder -> {
                val item =
                    placeTypeListItems[position] as? PlaceTypeListItem.PlaceCategory
                        ?: throw IllegalStateException()
                holder.placeCategoryNameChip.text = item.name
            }
            is ViewHolder.PlaceTypeItemViewHolder -> {
                val item =
                    placeTypeListItems[position] as? PlaceTypeListItem.PlaceType
                        ?: throw IllegalStateException()
                holder.placeTypeTextView.text = item.wrapped.label
                Glide.with(holder.itemView.context)
                    .load(item.drawableId)
                    .into(holder.placeTypeImageView)
                holder.itemView.setOnClickListener { onPlaceTypeClicked(item.wrapped) }
            }
        }
    }

    override fun getItemCount(): Int = placeTypeListItems.size

    override fun getItemViewType(position: Int): Int =
        when (placeTypeListItems[position]) {
            is PlaceTypeListItem.PlaceCategory -> ViewType.PLACE_CATEGORY_HEADER.ordinal
            is PlaceTypeListItem.PlaceType -> ViewType.PLACE_TYPE.ordinal
        }

    internal sealed class ViewHolder(parent: ViewGroup, layoutResource: Int) :
        RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(layoutResource, parent, false)
        ) {

        class PlaceTypeItemViewHolder(parent: ViewGroup, layoutResource: Int) :
            ViewHolder(parent, layoutResource) {
            val placeTypeTextView: TextView = itemView.findViewById(R.id.place_type_text)
            val placeTypeImageView: ImageView = itemView.findViewById(R.id.place_type_image)
        }

        class PlaceCategoryHeaderItemViewHolder(parent: ViewGroup, layoutResource: Int) :
            ViewHolder(parent, layoutResource) {
            val placeCategoryNameChip: Chip = itemView.findViewById(R.id.place_category_name_chip)
        }
    }

    private enum class ViewType {
        PLACE_TYPE,
        PLACE_CATEGORY_HEADER
    }
}
