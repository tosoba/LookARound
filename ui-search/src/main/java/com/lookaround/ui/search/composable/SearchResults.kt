package com.lookaround.ui.search.composable

import android.content.res.Configuration
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.model.Point
import com.lookaround.core.android.view.theme.LookARoundTheme
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
        item { SearchResultsTopSpacer() }

        if (!lastPerformedWithLocationPriority) {
            item {
                SearchResultInfoCard(
                    "WARNING - Search performed with no location priority.",
                    color = LookARoundTheme.colors.error
                )
            }
        }

        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            items(points.chunked(2)) { chunk ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.wrapContentHeight()
                ) {
                    chunk.forEach { point ->
                        SearchResult(point, locationFlow, Modifier.weight(1f))
                    }
                }
            }
        } else {
            items(points) { point -> SearchResult(point, locationFlow, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
internal fun SearchResultsTopSpacer() {
    Spacer(modifier = Modifier.height(60.dp))
}

@Composable
internal fun SearchResultInfoCard(text: String, color: Color) {
    SearchResultCard {
        Text(
            text,
            style = MaterialTheme.typography.subtitle2,
            color = color,
            modifier = Modifier.padding(10.dp).fillMaxWidth()
        )
    }
}

@Composable
private fun SearchResult(
    point: Point,
    locationFlow: Flow<Location>,
    modifier: Modifier = Modifier
) {
    SearchResultCard(modifier = modifier) {
        Column(modifier = Modifier.padding(5.dp)) {
            Text(
                text = point.name,
                style = MaterialTheme.typography.subtitle1,
                color = LookARoundTheme.colors.textPrimary,
                modifier = Modifier.heightIn(min = 20.dp).wrapContentHeight()
            )
            val location = locationFlow.collectAsState(null).value
            if (location != null) {
                Text(
                    text = point.location.formattedDistanceTo(location),
                    style = MaterialTheme.typography.subtitle2,
                    color = LookARoundTheme.colors.textSecondary,
                    modifier = Modifier.heightIn(min = 16.dp).wrapContentHeight()
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        modifier = modifier,
        content = content
    )
}
