package com.lookaround.ui.search.composable

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.model.Point
import com.lookaround.core.android.view.theme.LookARoundTheme

@Composable
internal fun SearchResults(points: List<Point>, modifier: Modifier = Modifier) {
    LazyColumn(modifier) {
        items(points) { item ->
            // TODO: distance away from current location?
            Text(
                text = item.name,
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
