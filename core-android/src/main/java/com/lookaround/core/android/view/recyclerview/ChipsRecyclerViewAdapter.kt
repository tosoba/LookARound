package com.lookaround.core.android.view.recyclerview

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lookaround.core.android.R
import com.lookaround.core.android.databinding.TransparentChipItemBinding
import com.lookaround.core.android.ext.darkMode
import com.lookaround.core.android.ext.dpToPx
import com.lookaround.core.android.ext.setListBackgroundItemDrawableWith
import com.lookaround.core.android.view.theme.Neutral7

class ChipsRecyclerViewAdapter<I>(
    private var items: List<I>,
    private val label: (I) -> String,
    private val transparent: Boolean = true,
    private val onMoreClicked: (() -> Unit)? = null,
    private val onItemClicked: (I) -> Unit
) : RecyclerView.Adapter<ViewBindingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewBindingViewHolder =
        ViewBindingViewHolder(
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
                    if (transparent) {
                        root.setListBackgroundItemDrawableWith(Color.WHITE)
                    } else {
                        root.setListBackgroundItemDrawableWith(
                            contrastingColor =
                                if (parent.context.darkMode) Color.parseColor("#ff121212")
                                else Color.WHITE,
                            alpha = 0xff
                        )
                        if (!parent.context.darkMode) {
                            chipLabelTextView.setTextColor(Neutral7.toArgb())
                        }
                    }
                    chipLabelTextView.textSize = 16f
                }
        )

    override fun onBindViewHolder(holder: ViewBindingViewHolder, position: Int) {
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
}
