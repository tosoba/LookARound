package com.lookaround.ui.place.list

import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.model.INamedLocation
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.composable.BottomSheetHeaderText
import com.lookaround.core.android.view.composable.PlaceItem
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map

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
    point: INamedLocation,
    userLocationFlow: Flow<Location>,
    getPlaceBitmap: suspend (Location) -> ReceiveChannel<Bitmap>,
    modifier: Modifier = Modifier
) {
    val bitmap = placeMapState(point = point, getPlaceMapBitmap = getPlaceBitmap)
    val distanceLabelState =
        userLocationFlow
            .map { point.location.formattedDistanceTo(it) }
            .collectAsState(initial = null)

    Column {
        if (bitmap.value is Success<Bitmap>) {
            val b = (bitmap.value as Success<Bitmap>).result
            val ib = b.asImageBitmap()
            Image(bitmap = ib, "", modifier)
        }
        distanceLabelState.value?.let { Text(text = it) }
        if (bitmap.value is Success<Bitmap>) {
            Text((bitmap.value as Success<Bitmap>).result.width.toString())
        }
    }
}

@Composable
private fun placeMapState(
    point: INamedLocation,
    getPlaceMapBitmap: suspend (Location) -> ReceiveChannel<Bitmap>,
): State<SimpleLoadable<Bitmap>> =
    produceState(initialValue = Loading, point) {
        getPlaceMapBitmap(point.location).consumeAsFlow().collect { value = Success(it) }
    }

private sealed class SimpleLoadable<out T>

private data class Success<out T>(val result: T) : SimpleLoadable<T>()

private object Error : SimpleLoadable<Nothing>()

private object Loading : SimpleLoadable<Nothing>()
