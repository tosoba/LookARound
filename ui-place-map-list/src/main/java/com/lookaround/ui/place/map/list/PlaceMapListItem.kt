package com.lookaround.ui.place.map.list

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.model.INamedLocation
import com.lookaround.core.android.view.composable.ItemDistanceText
import com.lookaround.core.android.view.composable.ItemNameText
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
    elevation: Dp,
    backgroundColor: Color,
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

    Card(
        elevation = elevation,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = backgroundColor,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Card(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(bitmapDimension.dp),
            ) {
                val currentBitmap = bitmap
                if (currentBitmap != null) MapImage(currentBitmap) else ShimmerAnimation()
            }
            ItemNameText(
                point.name,
                modifier = Modifier.padding(vertical = 5.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            userLocationState.value?.let { userLocation ->
                ItemDistanceText(location1 = point.location, location2 = userLocation)
            }
        }
    }
}

@Composable
private fun MapImage(bitmap: Bitmap, modifier: Modifier = Modifier) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}
