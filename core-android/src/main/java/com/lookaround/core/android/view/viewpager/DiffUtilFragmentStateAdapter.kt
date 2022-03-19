package com.lookaround.core.android.view.viewpager

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
class DiffUtilFragmentStateAdapter(
    activity: FragmentActivity,
    fragmentFactories: List<ViewPagerFragmentFactory> = emptyList(),
) : FragmentStateAdapter(activity) {
    var fragmentFactories: List<ViewPagerFragmentFactory> = fragmentFactories
        @MainThread
        set(value) {
            val callback = DiffUtilCallback(field, value)
            val diff = DiffUtil.calculateDiff(callback)
            field = value
            diff.dispatchUpdatesTo(this)
        }

    override fun createFragment(position: Int): Fragment = fragmentFactories[position].newInstance()
    override fun getItemCount() = fragmentFactories.size
    override fun getItemId(position: Int): Long = fragmentFactories[position].fragmentId
    override fun containsItem(itemId: Long): Boolean =
        fragmentFactories.any { it.fragmentId == itemId }

    private class DiffUtilCallback(
        private val oldList: List<ViewPagerFragmentFactory>,
        private val newList: List<ViewPagerFragmentFactory>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].fragmentId == newList[newItemPosition].fragmentId

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }
}
