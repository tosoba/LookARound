package com.lookaround.core.android.view.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.R
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.ext.titleCaseWithSpacesInsteadOfUnderscores
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@ExperimentalMaterialApi
@Composable
fun <I> ChipList(
    itemsFlow: Flow<List<I>>,
    label: (I) -> String,
    modifier: Modifier = Modifier,
    chipModifier: Modifier = Modifier,
    onMoreClicked: (() -> Unit)? = null,
    onItemClicked: (I) -> Unit
) {
    val items = itemsFlow.collectAsState(initial = emptyList())
    if (items.value.isEmpty()) return

    val lazyListState = rememberLazyListState()
    LazyRow(state = lazyListState, modifier = modifier) {
        item { Box(modifier = Modifier.size(10.dp)) }
        items(items.value) { item ->
            Chip(
                onClick = { onItemClicked(item) },
                colors = ChipDefaults.chipColors(backgroundColor = Color.Transparent),
                modifier = Modifier.padding(horizontal = 2.dp).then(chipModifier)
            ) {
                Text(
                    text = label(item).titleCaseWithSpacesInsteadOfUnderscores,
                    color = LookARoundTheme.colors.textLink
                )
            }
        }
        if (onMoreClicked != null) {
            item {
                Chip(
                    onClick = onMoreClicked,
                    colors = ChipDefaults.chipColors(backgroundColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.more),
                        color = LookARoundTheme.colors.textLink
                    )
                }
            }
        }
        item { Box(modifier = Modifier.size(10.dp)) }
    }
}
