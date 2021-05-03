package com.lookaround.ui.place.list

import android.graphics.Bitmap
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lookaround.core.android.model.Marker
import java.lang.ref.WeakReference
import kotlinx.coroutines.channels.SendChannel

internal class PlaceMapListAdapter(
    private val bindViewHolderEventsChannel: SendChannel<Pair<Location, (Bitmap) -> Unit>>
) : RecyclerView.Adapter<PlaceMapListViewHolder>() {
    private val asyncListDiffer = AsyncListDiffer(this, PlaceMapListDiffUtilItemCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceMapListViewHolder =
        PlaceMapListViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.place_map_list_item, parent, false)
        )

    override fun onBindViewHolder(holder: PlaceMapListViewHolder, position: Int) {
        val view = WeakReference(holder.view)
        bindViewHolderEventsChannel.offer(
            asyncListDiffer.currentList[position].location to
                { bitmap ->
                    view.get()
                        ?.findViewById<ImageView>(R.id.place_map_image_view)
                        ?.setImageBitmap(bitmap)
                }
        )
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    fun update(newList: List<Marker>) {
        asyncListDiffer.submitList(newList)
    }
}

internal class PlaceMapListViewHolder(val view: View) : RecyclerView.ViewHolder(view)

private object PlaceMapListDiffUtilItemCallback : DiffUtil.ItemCallback<Marker>() {
    override fun areItemsTheSame(oldItem: Marker, newItem: Marker): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Marker, newItem: Marker): Boolean =
        areItemsTheSame(oldItem, newItem)
}
