package com.lookaround.ui.place.list

import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.composable.BottomSheetHeaderText
import com.lookaround.core.android.view.composable.PlaceItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Composable
fun PlacesList(markers: List<Marker>, locationFlow: Flow<Location>, modifier: Modifier = Modifier) {
    Column(modifier) {
        BottomSheetHeaderText("Places")
        val configuration = LocalConfiguration.current
        LazyColumn(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                markers.chunked(
                    size =
                        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3
                        else 2
                )
            ) { chunk ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.wrapContentHeight()
                ) { chunk.forEach { point -> PlaceItem(point, locationFlow, Modifier.weight(1f)) } }
            }
        }
    }
}

@Composable
internal fun PlaceMapListItem(
    location: Location,
    userLocationFlow: Flow<Location>,
    capturePlaceMap: suspend (Location) -> Bitmap,
    modifier: Modifier = Modifier
) {
    var captureJob: Job?
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val userLocation = userLocationFlow.collectAsState(initial = null)
    var t by remember { mutableStateOf(0) }

    DisposableEffect(key1 = location) {
        captureJob =
            scope.launch {
                launch {
                    val b = capturePlaceMap(location)
                    bitmap = b
                    t = b.byteCount
                }
                userLocationFlow
                    .onEach {
                        // TODO: set distance text
                    }
                    .launchIn(scope)
            }
        onDispose { captureJob?.cancel() }
    }

    bitmap?.let { loadedBitmap ->
        Column {
            Image(bitmap = loadedBitmap.asImageBitmap(), "", modifier)
            userLocation.value?.let { Text(location.formattedDistanceTo(it)) }
            Text(t.toString())
        }
    }
        ?: run {
            // TODO loading placeholder (with shimmer or smth)
            Column {
                userLocation.value?.let { Text(location.formattedDistanceTo(it)) }
                Text(t.toString())
            }
        }
}
