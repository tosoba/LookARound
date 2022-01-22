package com.lookaround.ui.place.list

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.model.INamedLocation
import com.lookaround.core.android.view.composable.ItemDistanceText
import com.lookaround.core.android.view.composable.ItemNameText
import com.lookaround.core.android.view.composable.LookARoundCard
import com.lookaround.core.android.view.composable.ShimmerAnimation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
internal fun PlaceMapListItem(
    point: INamedLocation,
    userLocationFlow: Flow<Location>,
    getPlaceBitmap: suspend (Location) -> Bitmap,
    reloadBitmapTrigger: Flow<Unit>,
    bitmapDimension: Int,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(key1 = point.location) { bitmap = getPlaceBitmap(point.location) }
    LaunchedEffect(key1 = point.location) {
        reloadBitmapTrigger
            .onEach {
                bitmap = null
                bitmap = getPlaceBitmap(point.location)
            }
            .launchIn(this)
    }
    val userLocationState = userLocationFlow.collectAsState(initial = null)

    LookARoundCard(
        backgroundColor = Color.White.copy(alpha = .85f),
        elevation = 0.dp,
        modifier = modifier
    ) {
        Column {
            val bitmapModifier = Modifier.size(bitmapDimension.dp)
            if (bitmap != null) MapImage(bitmap!!, bitmapModifier)
            else ShimmerAnimation(bitmapModifier)
            ItemNameText(point.name, modifier = Modifier.padding(5.dp))
            userLocationState.value?.let { userLocation ->
                ItemDistanceText(
                    location1 = point.location,
                    location2 = userLocation,
                    modifier = Modifier.padding(5.dp)
                )
            }
        }
    }
}

@Composable
private fun MapImage(bitmap: Bitmap, modifier: Modifier) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}
