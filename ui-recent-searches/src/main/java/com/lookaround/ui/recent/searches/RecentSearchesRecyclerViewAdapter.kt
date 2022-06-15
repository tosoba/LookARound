package com.lookaround.ui.recent.searches

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.lookaround.core.android.databinding.SpacerItemBinding
import com.lookaround.core.android.ext.getDrawableOfPlaceTypeBy
import com.lookaround.core.android.ext.roundedFormattedDistanceTo
import com.lookaround.core.android.ext.setListBackgroundItemDrawableWith
import com.lookaround.core.android.view.recyclerview.DefaultDiffUtilCallback
import com.lookaround.core.android.view.recyclerview.LocationRecyclerViewAdapterCallbacks
import com.lookaround.core.ext.titleCaseWithSpacesInsteadOfUnderscores
import com.lookaround.core.model.SearchType
import com.lookaround.ui.recent.searches.databinding.RecentSearchListItemBinding
import com.lookaround.ui.recent.searches.model.RecentSearchModel

class RecentSearchesRecyclerViewAdapter(
    private val userLocationCallbacks: LocationRecyclerViewAdapterCallbacks<Long>,
    private val onItemLongClicked: (RecentSearchModel) -> Unit,
    private val onItemClicked: (RecentSearchModel) -> Unit,
) : RecyclerView.Adapter<RecentSearchesRecyclerViewAdapter.ViewHolder>() {
    var items: List<Item> = emptyList()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(
            when (ViewType.values()[viewType]) {
                ViewType.SPACER -> {
                    SpacerItemBinding.inflate(inflater, parent, false).apply {
                        root.layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                (items[0] as Item.Spacer).heightPx
                            )
                    }
                }
                ViewType.SEARCH -> {
                    RecentSearchListItemBinding.inflate(inflater, parent, false).apply {
                        root.setListBackgroundItemDrawableWith(Color.WHITE)
                    }
                }
            }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        if (item is Item.Search) bindSearchItem(holder, item)
    }

    private fun bindSearchItem(holder: ViewHolder, item: Item.Search) {
        val binding = holder.binding as RecentSearchListItemBinding
        when (item.search.type) {
            SearchType.AROUND -> {
                binding.root.context.getDrawableOfPlaceTypeBy(item.search.label)?.let {
                    binding.recentSearchImage.setImageDrawable(it)
                }
            }
            SearchType.AUTOCOMPLETE -> {
                binding.recentSearchImage.setImageDrawable(
                    ContextCompat.getDrawable(binding.root.context, R.drawable.text_search)
                )
                binding.recentSearchImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        }
        binding.recentSearchNameText.text =
            item.search.label.titleCaseWithSpacesInsteadOfUnderscores
        binding.recentSearchTimestampText.text =
            TimeAgo.using(item.search.lastSearchedAt.time).replace("about", "")
        item.search.location?.let {
            userLocationCallbacks.onBindViewHolder(item.search.id) { userLocation ->
                binding.recentSearchDistanceText.text = userLocation.roundedFormattedDistanceTo(it)
            }
        }
        binding.root.setOnClickListener { onItemClicked(item.search) }
        binding.root.setOnLongClickListener {
            onItemLongClicked(item.search)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is Item.Spacer -> ViewType.SPACER.ordinal
            is Item.Search -> ViewType.SEARCH.ordinal
        }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        userLocationCallbacks.onDetachedFromRecyclerView()
    }

    fun updateItems(newItems: List<Item>) {
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    fun addTopSpacer(spacer: Item.Spacer) {
        val newItems = ArrayList(items).apply { add(0, spacer) }
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    sealed interface Item {
        data class Search(val search: RecentSearchModel) : Item
        data class Spacer(val heightPx: Int) : Item
    }

    class ViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    private enum class ViewType {
        SPACER,
        SEARCH,
    }
}
