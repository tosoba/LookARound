package com.lookaround.ui.place.map.list

import android.graphics.Bitmap
import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.ext.setListBackgroundItemDrawableWith
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.recyclerview.ColorRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.recyclerview.DefaultDiffUtilCallback
import com.lookaround.ui.place.list.databinding.PlaceMapListItemBinding
import java.util.*

internal class PlaceMapsRecyclerViewAdapter(
    private var items: List<Marker>,
    private val colorCallbacks: ColorRecyclerViewAdapterCallbacks,
    private val bitmapCallbacks: BitmapCallbacks,
    private val userLocationCallbacks: UserLocationCallbacks,
    private val onItemClicked: (Marker) -> Unit,
) : RecyclerView.Adapter<PlaceMapsRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            PlaceMapListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

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
        holder.binding.placeMapNameText.text = item.name
        bitmapCallbacks.onBindViewHolder(holder.uuid, item.location) { bitmap ->
            holder.binding.placeMapImage.setImageBitmap(bitmap)
        }
        userLocationCallbacks.onBindViewHolder(holder.uuid) { userLocation ->
            holder.binding.placeMapDistanceText.text =
                userLocation.formattedDistanceTo(item.location)
        }
        holder.binding.root.setOnClickListener { onItemClicked(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<Marker>) {
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    class ViewHolder(
        val binding: PlaceMapListItemBinding,
        val uuid: UUID = UUID.randomUUID(),
    ) : RecyclerView.ViewHolder(binding.root)

    interface UserLocationCallbacks {
        fun onBindViewHolder(uuid: UUID, action: (userLocation: Location) -> Unit)
        fun onViewDetachedFromWindow(uuid: UUID)
        fun onDetachedFromRecyclerView()
    }

    interface BitmapCallbacks {
        fun onBindViewHolder(uuid: UUID, location: Location, action: (bitmap: Bitmap) -> Unit)
        fun onViewDetachedFromWindow(uuid: UUID)
        fun onDetachedFromRecyclerView()
    }
}
