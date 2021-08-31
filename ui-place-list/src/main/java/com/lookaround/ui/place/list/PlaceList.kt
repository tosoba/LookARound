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
import com.lookaround.core.android.model.INamedLocation
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.composable.BottomSheetHeaderText
import com.lookaround.core.android.view.composable.PlaceItem
import kotlinx.coroutines.flow.Flow
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
    capturePlaceMap: suspend (Location) -> Bitmap,
    modifier: Modifier = Modifier
) {
    val bitmap = placeMapState(point = point, getPlaceMapBitmap = capturePlaceMap)
    val distanceLabelState =
        userLocationFlow
            .map { point.location.formattedDistanceTo(it) }
            .collectAsState(initial = null)

    if (bitmap.value is Success<Bitmap>) {
        Column {
            Image(bitmap = (bitmap.value as Success<Bitmap>).result.asImageBitmap(), "", modifier)
            distanceLabelState.value?.let { Text(text = it) }
            Text((bitmap.value as Success<Bitmap>).result.width.toString())
        }
    } else {
        // TODO loading placeholder (with shimmer or smth)
        Column { distanceLabelState.value?.let { Text(text = it) } }
    }
}

@Composable
private fun placeMapState(
    point: INamedLocation,
    getPlaceMapBitmap: suspend (Location) -> Bitmap,
): State<SimpleLoadable<Bitmap>> =
    produceState(initialValue = Loading, point) {
        val image = getPlaceMapBitmap(point.location)
        value = Success(image)
    }

private sealed class SimpleLoadable<out T>

private data class Success<out T>(val result: T) : SimpleLoadable<T>()

private object Error : SimpleLoadable<Nothing>()

private object Loading : SimpleLoadable<Nothing>()
