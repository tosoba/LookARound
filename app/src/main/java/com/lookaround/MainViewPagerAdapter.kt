package com.lookaround

import androidx.annotation.MainThread
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
class MainViewPagerAdapter(
    activity: FragmentActivity,
    items: List<MainFragment>,
) : FragmentStateAdapter(activity) {
    var items: List<MainFragment> = items
        @MainThread
        set(value) {
            val callback = PagerDiffUtil(field, value)
            val diff = DiffUtil.calculateDiff(callback)
            field = value
            diff.dispatchUpdatesTo(this)
        }

    override fun createFragment(position: Int): Fragment = items[position].newInstance()
    override fun getItemCount() = items.size
    override fun getItemId(position: Int): Long = items[position].ordinal.toLong()
    override fun containsItem(itemId: Long): Boolean = items.any { it.ordinal.toLong() == itemId }

    class PagerDiffUtil(
        private val oldList: List<MainFragment>,
        private val newList: List<MainFragment>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].ordinal == newList[newItemPosition].ordinal

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }
}
