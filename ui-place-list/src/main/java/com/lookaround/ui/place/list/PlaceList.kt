package com.lookaround.ui.place.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.view.composable.BottomSheetHeaderText
import com.lookaround.core.android.view.composable.LookARoundSurface

@Composable
fun PlacesList(markers: List<Marker>, modifier: Modifier = Modifier) {
    Column(modifier) {
        BottomSheetHeaderText("Places")
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            val itemCount = if (markers.size % 2 == 0) markers.size / 2 else markers.size / 2 + 1
            items(itemCount) { PlacesListItemsRow(rowIndex = it, markers = markers) }
        }
    }
}

@Composable
private fun PlacesListItemsRow(rowIndex: Int, markers: List<Marker>) {
    Column {
        Row {
            PlacesListItem(marker = markers[rowIndex * 2], modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            if (markers.size >= rowIndex * 2 + 2) {
                PlacesListItem(marker = markers[rowIndex * 2 + 1], modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PlacesListItem(marker: Marker, modifier: Modifier = Modifier, elevation: Dp = 0.dp) {
    LookARoundSurface(
        color = Color.LightGray,
        elevation = elevation,
        shape = CircleShape,
        modifier = modifier
    ) { Text(text = marker.name, modifier = Modifier.fillMaxSize()) }
}
