package com.lookaround.ui.place.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lookaround.ui.place.categories.model.PlaceType

class PlaceTypesRecyclerViewAdapter(private val placeTypes: List<PlaceType>) :
    RecyclerView.Adapter<PlaceTypesRecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.place_type_item, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.place_type_text).text =
            placeTypes[position].wrapped.label
        Glide.with(holder.itemView.context)
            .load(placeTypes[position].drawableId)
            .into(holder.itemView.findViewById(R.id.place_type_image))
    }

    override fun getItemCount(): Int = placeTypes.size
}
