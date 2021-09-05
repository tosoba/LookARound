package com.lookaround.ui.place.list

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.model.INamedLocation
import com.lookaround.core.android.view.composable.LookARoundCard
import com.lookaround.core.android.view.composable.PlaceItemDistanceText
import com.lookaround.core.android.view.composable.PlaceItemNameText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
internal fun PlaceMapListItem(
    point: INamedLocation,
    userLocationFlow: Flow<Location>,
    getPlaceBitmap: suspend (Location) -> Bitmap,
    reloadBitmapTrigger: Flow<Unit>,
    modifier: Modifier = Modifier
) {
    var bitmapState by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(key1 = point.location) { bitmapState = getPlaceBitmap(point.location) }
    LaunchedEffect(key1 = point.location) {
        reloadBitmapTrigger
            .onEach {
                bitmapState = null
                bitmapState = getPlaceBitmap(point.location)
            }
            .launchIn(this)
    }
    val userLocationState = userLocationFlow.collectAsState(initial = null)

    LookARoundCard(modifier = modifier) {
        Column(modifier = Modifier.padding(5.dp)) {
            bitmapState?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = point.location.toString(),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
            PlaceItemNameText(point)
            userLocationState.value?.let { userLocation ->
                PlaceItemDistanceText(point = point, location = userLocation)
            }
        }
    }
}
