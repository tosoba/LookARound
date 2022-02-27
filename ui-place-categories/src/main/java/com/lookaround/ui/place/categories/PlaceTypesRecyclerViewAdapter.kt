package com.lookaround.ui.place.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
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
        holder
            .itemView
            .findViewById<ImageView>(R.id.place_type_image)
            .setImageDrawable(
                ContextCompat.getDrawable(holder.itemView.context, placeTypes[position].drawableId)
            )
    }

    override fun getItemCount(): Int = placeTypes.size
}
