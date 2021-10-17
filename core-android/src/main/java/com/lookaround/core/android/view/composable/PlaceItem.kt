package com.lookaround.core.android.view.composable

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.ext.formattedDistanceTo
import com.lookaround.core.android.model.INamedLocation
import com.lookaround.core.android.view.theme.LookARoundTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun PlaceItem(point: INamedLocation, locationFlow: Flow<Location>, modifier: Modifier = Modifier) {
    LookARoundCard(modifier = modifier) {
        Column(modifier = Modifier.padding(5.dp)) {
            PlaceItemNameText(point.name)
            val location = locationFlow.collectAsState(null).value
            if (location != null) PlaceItemDistanceText(point, location)
        }
    }
}

@Composable
fun PlaceItemDistanceText(
    point: INamedLocation,
    location: Location,
    modifier: Modifier = Modifier
) {
    Text(
        text = point.location.formattedDistanceTo(location),
        style = MaterialTheme.typography.subtitle2,
        color = LookARoundTheme.colors.textSecondary,
        modifier = Modifier.heightIn(min = 16.dp).wrapContentHeight() then modifier
    )
}

@Composable
fun PlaceItemNameText(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.subtitle1,
        color = LookARoundTheme.colors.textPrimary,
        modifier = Modifier.heightIn(min = 20.dp).wrapContentHeight() then modifier
    )
}

@Composable
fun PlaceInfoItem(text: String, color: Color, modifier: Modifier = Modifier) {
    LookARoundCard(modifier) {
        Text(
            text,
            style = MaterialTheme.typography.subtitle2,
            color = color,
            modifier = Modifier.padding(10.dp).fillMaxWidth()
        )
    }
}

@Composable
fun LookARoundCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    elevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit
) {
    Card(
        elevation = elevation,
        shape = shape,
        backgroundColor = backgroundColor,
        modifier = modifier,
        content = content
    )
}
