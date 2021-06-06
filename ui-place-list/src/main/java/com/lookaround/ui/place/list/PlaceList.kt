package com.lookaround.ui.place.list

import android.content.res.Configuration
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.composable.BottomSheetHeaderText
import com.lookaround.core.android.view.composable.PlaceItem
import kotlinx.coroutines.flow.Flow

@Composable
fun PlacesList(markers: List<Marker>, locationFlow: Flow<Location>, modifier: Modifier = Modifier) {
    Column(modifier) {
        BottomSheetHeaderText("Places")
        val configuration = LocalConfiguration.current
        LazyColumn(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                items(markers.chunked(2)) { chunk ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.wrapContentHeight()
                    ) {
                        chunk.forEach { point ->
                            PlaceItem(point, locationFlow, Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(markers) { point -> PlaceItem(point, locationFlow, Modifier.fillMaxWidth()) }
            }
        }
    }
}
