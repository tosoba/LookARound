package com.lookaround

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
class MainViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    private val items: ArrayList<MainFragment> = arrayListOf()

    override fun createFragment(position: Int): Fragment = items[position].newInstance()
    override fun getItemCount() = items.size
    override fun getItemId(position: Int): Long = items[position].ordinal.toLong()
    override fun containsItem(itemId: Long): Boolean = items.any { it.ordinal.toLong() == itemId }

    fun setItems(newItems: List<MainFragment>) {
        val callback = PagerDiffUtil(items, newItems)
        val diff = DiffUtil.calculateDiff(callback)

        items.clear()
        items.addAll(newItems)

        diff.dispatchUpdatesTo(this)
    }

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
