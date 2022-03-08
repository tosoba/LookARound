package com.lookaround.core.android.view.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.lookaround.core.android.R
import com.lookaround.core.android.databinding.TransparentChipItemBinding
import java.util.*

class TransparentChipsRecyclerViewAdapter<I>(
    private var items: List<I>,
    private val label: (I) -> String,
    private val onMoreClicked: (() -> Unit)? = null,
    private val onItemClicked: (I) -> Unit
) : RecyclerView.Adapter<TransparentChipsRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            TransparentChipItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.binding !is TransparentChipItemBinding) throw IllegalStateException()
        with(holder.binding) {
            chipLabelTextView.text =
                if (position < items.size) label(items[position])
                else root.resources.getString(R.string.more)
            root.isClickable = true
            root.isFocusable = true
            root.setOnClickListener {
                if (position < items.size) onItemClicked(items[position])
                else onMoreClicked?.invoke()
            }
        }
    }

    override fun getItemCount(): Int = if (onMoreClicked != null) items.size + 1 else items.size

    fun updateItems(newItems: List<I>) {
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    class ViewHolder(
        val binding: ViewBinding,
        val uuid: UUID = UUID.randomUUID(),
    ) : RecyclerView.ViewHolder(binding.root)
}
