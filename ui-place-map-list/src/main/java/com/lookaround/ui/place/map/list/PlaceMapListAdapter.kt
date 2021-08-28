package com.lookaround.ui.place.map.list

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.model.Marker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

internal class PlaceMapListAdapter(
    private val captureRequestChannel: SendChannel<PlaceMapCaptureRequest>,
    private val locationFlow: Flow<Location>,
    private val onItemClick: (Marker) -> Unit
) : RecyclerView.Adapter<PlaceMapListAdapter.ViewHolder>() {
    private val asyncListDiffer = AsyncListDiffer(this, DiffUtilItemCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.place_map_list_item, parent, false),
            )
            .apply {
                view.setOnClickListener {
                    val position = adapterPosition
                    if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                    val marker = asyncListDiffer.currentList[position]
                    onItemClick(marker)
                }
            }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val marker = asyncListDiffer.currentList[position]

        holder.nameTextView.text = marker.name
        locationFlow
            .onEach { holder.distanceTextView.text = marker.location.formattedDistanceTo(it) }
            .launchIn(holder)

        captureRequestChannel.offer(
            PlaceMapCaptureRequest(location = marker.location, holder = WeakReference(holder))
        )
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    override fun onViewRecycled(holder: ViewHolder) {
        holder.cancel()
    }

    fun update(newList: List<Marker>) {
        asyncListDiffer.submitList(newList)
    }

    internal class ViewHolder(val view: View) : RecyclerView.ViewHolder(view), CoroutineScope {
        private val job = SupervisorJob()
        override val coroutineContext: CoroutineContext
            get() = job

        val placeMapImageView: ImageView
            get() = view.findViewById(R.id.place_map_image_view)
        val nameTextView: TextView
            get() = view.findViewById(R.id.place_map_name_text_view)
        val distanceTextView: TextView
            get() = view.findViewById(R.id.place_map_distance_text_view)
    }

    private object DiffUtilItemCallback : DiffUtil.ItemCallback<Marker>() {
        override fun areItemsTheSame(oldItem: Marker, newItem: Marker): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Marker, newItem: Marker): Boolean =
            areItemsTheSame(oldItem, newItem)
    }
}
