package com.lookaround.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.lookaround.core.android.view.theme.LookARoundTheme

class SearchFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                LazyColumn {
                    items(items) { item ->
                        Text(
                            text = item,
                            style = MaterialTheme.typography.h6,
                            color = LookARoundTheme.colors.textPrimary,
                            modifier =
                                Modifier.heightIn(min = 24.dp)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .wrapContentHeight()
                        )
                    }
                }
            }
        }

    companion object {
        private val items =
            generateSequence { "RESULT" }
                .take(15)
                .mapIndexed { index, result -> "$result$index" }
                .toMutableList()
    }
}
