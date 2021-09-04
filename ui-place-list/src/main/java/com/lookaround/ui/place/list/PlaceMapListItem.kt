package com.lookaround.ui.place.list

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.model.INamedLocation
import com.lookaround.core.android.view.composable.LookARoundCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
internal fun PlaceMapListItem(
    point: INamedLocation,
    userLocationFlow: Flow<Location>,
    getPlaceBitmap: suspend (Location) -> Bitmap,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(key1 = point.location) { bitmap = getPlaceBitmap(point.location) }

    val distanceLabelState =
        userLocationFlow
            .map { userLocation -> point.location.formattedDistanceTo(userLocation) }
            .collectAsState(initial = null)

    LookARoundCard(modifier = modifier) {
        Column(modifier = Modifier.padding(5.dp)) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = point.location.toString(),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
            distanceLabelState.value?.let { Text(text = it) }
        }
    }
}
