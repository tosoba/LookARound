package com.lookaround.ui.place.list

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.ext.setListBackgroundItemDrawableWith
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.recyclerview.DefaultDiffUtilCallback
import com.lookaround.core.android.view.recyclerview.LocationRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.recyclerview.smoothScrollToCenteredPosition
import com.lookaround.ui.place.list.databinding.PlaceListItemBinding
import java.util.*

internal class PlacesRecyclerViewAdapter(
    private val userLocationCallbacks: LocationRecyclerViewAdapterCallbacks,
    private val onItemClicked: (Marker) -> Unit,
) : RecyclerView.Adapter<PlacesRecyclerViewAdapter.ViewHolder>() {
    var items: List<Marker> = emptyList()
        private set
    private var recyclerView: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            PlaceListItemBinding.inflate(inflater, parent, false).apply {
                root.setListBackgroundItemDrawableWith(Color.WHITE)
            }
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        userLocationCallbacks.onDetachedFromRecyclerView()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val marker = items[position]
        val binding = holder.binding as PlaceListItemBinding
        binding.placeNameText.text = marker.name
        userLocationCallbacks.onBindViewHolder(holder.uuid) { userLocation ->
            binding.placeDistanceText.text = userLocation.formattedDistanceTo(marker.location)
        }
        binding.root.setOnClickListener {
            recyclerView?.smoothScrollToCenteredPosition(position)
            onItemClicked(marker)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<Marker>) {
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    class ViewHolder(
        val binding: ViewBinding,
        val uuid: UUID = UUID.randomUUID(),
    ) : RecyclerView.ViewHolder(binding.root)
}
