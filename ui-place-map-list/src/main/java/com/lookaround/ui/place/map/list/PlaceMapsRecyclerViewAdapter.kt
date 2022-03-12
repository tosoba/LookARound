package com.lookaround.ui.place.map.list

import android.graphics.Bitmap
import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.lookaround.core.android.databinding.SpacerItemBinding
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.ext.setListBackgroundItemDrawableWith
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.recyclerview.ColorRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.recyclerview.DefaultDiffUtilCallback
import com.lookaround.ui.place.list.databinding.PlaceMapListItemBinding
import java.util.*

internal class PlaceMapsRecyclerViewAdapter(
    private val colorCallbacks: ColorRecyclerViewAdapterCallbacks,
    private val bitmapCallbacks: BitmapCallbacks,
    private val userLocationCallbacks: UserLocationCallbacks,
    private val onItemClicked: (Marker) -> Unit,
) : RecyclerView.Adapter<PlaceMapsRecyclerViewAdapter.ViewHolder>() {
    var items: List<Item> = emptyList()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            when (ViewType.values()[viewType]) {
                ViewType.SPACER -> {
                    SpacerItemBinding.inflate(inflater, parent, false).apply {
                        root.layoutParams =
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                (items[0] as Item.Spacer).heightPx
                            )
                    }
                }
                ViewType.MAP -> {
                    PlaceMapListItemBinding.inflate(inflater, parent, false)
                }
            }
        )
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        colorCallbacks.onViewAttachedToWindow(holder.uuid) { contrastingColor ->
            holder.binding.root.setListBackgroundItemDrawableWith(contrastingColor)
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        colorCallbacks.onViewDetachedFromWindow(holder.uuid)
        bitmapCallbacks.onViewDetachedFromWindow(holder.uuid)
        userLocationCallbacks.onViewDetachedFromWindow(holder.uuid)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        colorCallbacks.onDetachedFromRecyclerView()
        bitmapCallbacks.onDetachedFromRecyclerView()
        userLocationCallbacks.onDetachedFromRecyclerView()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        if (item is Item.Map) bindMapItem(holder, item)
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is Item.Spacer -> ViewType.SPACER.ordinal
            is Item.Map -> ViewType.MAP.ordinal
        }

    private fun bindMapItem(holder: ViewHolder, item: Item.Map) {
        val binding = holder.binding as PlaceMapListItemBinding
        binding.placeMapNameText.text = item.marker.name
        bitmapCallbacks.onBindViewHolder(
            holder.uuid,
            item.marker.location,
            onBitmapLoadingStarted = { binding.placeMapImage.background = null },
            onBitmapLoaded = { bitmap -> binding.placeMapImage.setImageBitmap(bitmap) }
        )
        userLocationCallbacks.onBindViewHolder(holder.uuid) { userLocation ->
            binding.placeMapDistanceText.text =
                userLocation.formattedDistanceTo(item.marker.location)
        }
        binding.root.setOnClickListener { onItemClicked(item.marker) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<Item>) {
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    fun addTopSpacer(spacer: Item.Spacer) {
        val newItems = ArrayList(items).apply { add(0, spacer) }
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    class ViewHolder(
        val binding: ViewBinding,
        val uuid: UUID = UUID.randomUUID(),
    ) : RecyclerView.ViewHolder(binding.root)

    private enum class ViewType {
        SPACER,
        MAP,
    }

    sealed interface Item {
        data class Map(val marker: Marker) : Item
        data class Spacer(val heightPx: Int) : Item
    }

    interface UserLocationCallbacks {
        fun onBindViewHolder(uuid: UUID, action: (userLocation: Location) -> Unit)
        fun onViewDetachedFromWindow(uuid: UUID)
        fun onDetachedFromRecyclerView()
    }

    interface BitmapCallbacks {
        fun onBindViewHolder(
            uuid: UUID,
            location: Location,
            onBitmapLoadingStarted: () -> Unit,
            onBitmapLoaded: (bitmap: Bitmap) -> Unit
        )
        fun onViewDetachedFromWindow(uuid: UUID)
        fun onDetachedFromRecyclerView()
    }
}