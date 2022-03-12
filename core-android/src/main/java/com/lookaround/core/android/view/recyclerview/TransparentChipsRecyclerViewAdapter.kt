package com.lookaround.core.android.view.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.lookaround.core.android.R
import com.lookaround.core.android.databinding.TransparentChipItemBinding
import com.lookaround.core.android.ext.dpToPx
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
                .apply {
                    root.isClickable = true
                    root.isFocusable = true
                    root.updateLayoutParams<RecyclerView.LayoutParams> {
                        val margin = root.context.dpToPx(4f).toInt()
                        when (ViewType.values()[viewType]) {
                            ViewType.FIRST -> {
                                leftMargin = margin * 3
                                rightMargin = margin
                            }
                            ViewType.LAST -> {
                                leftMargin = margin
                                rightMargin = margin * 3
                            }
                            ViewType.OTHER -> {
                                leftMargin = margin
                                rightMargin = margin
                            }
                        }
                    }
                }
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.binding !is TransparentChipItemBinding) throw IllegalStateException()
        with(holder.binding) {
            chipLabelTextView.text =
                if (position < items.size) label(items[position])
                else root.resources.getString(R.string.more)
            root.setOnClickListener {
                if (position < items.size) onItemClicked(items[position])
                else onMoreClicked?.invoke()
            }
        }
    }

    override fun getItemCount(): Int = if (onMoreClicked != null) items.size + 1 else items.size

    override fun getItemViewType(position: Int): Int {
        val lastIndex = if (onMoreClicked == null) items.size - 1 else items.size
        return when (position) {
            0 -> ViewType.FIRST.ordinal
            lastIndex -> ViewType.LAST.ordinal
            else -> ViewType.OTHER.ordinal
        }
    }

    fun updateItems(newItems: List<I>) {
        DiffUtil.calculateDiff(DefaultDiffUtilCallback(items, newItems)).dispatchUpdatesTo(this)
        items = newItems
    }

    private enum class ViewType {
        FIRST,
        LAST,
        OTHER
    }

    class ViewHolder(
        val binding: ViewBinding,
        val uuid: UUID = UUID.randomUUID(),
    ) : RecyclerView.ViewHolder(binding.root)
}
