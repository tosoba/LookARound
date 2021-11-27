package com.lookaround.ui.search.composable

import android.content.res.Configuration
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.model.Point
import com.lookaround.core.android.view.composable.ListTopSpacer
import com.lookaround.core.android.view.composable.InfoItem
import com.lookaround.core.android.view.composable.PlaceItem
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.search.R
import kotlinx.coroutines.flow.Flow

@Composable
internal fun SearchResults(
    points: List<Point>,
    lastPerformedWithLocationPriority: Boolean,
    locationFlow: Flow<Location>,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ListTopSpacer() }

        if (!lastPerformedWithLocationPriority) {
            item {
                InfoItem(
                    stringResource(R.string.search_no_location_priority_warning),
                    color = LookARoundTheme.colors.error
                )
            }
        }

        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            items(points.chunked(2)) { chunk ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.wrapContentHeight()
                ) { chunk.forEach { point -> PlaceItem(point, locationFlow, Modifier.weight(1f)) } }
            }
        } else {
            items(points) { point -> PlaceItem(point, locationFlow, Modifier.fillMaxWidth()) }
        }
    }
}
