package com.lookaround.core.android.view.composable

import android.location.Location
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.ext.preciseFormattedDistanceTo
import com.lookaround.core.android.view.theme.LookARoundTheme

@Composable
fun ItemDistanceText(location1: Location, location2: Location, modifier: Modifier = Modifier) {
    Text(
        text = location1.preciseFormattedDistanceTo(location2),
        style = MaterialTheme.typography.subtitle2,
        color = LookARoundTheme.colors.textSecondary,
        modifier = Modifier.heightIn(min = 16.dp).wrapContentHeight() then modifier
    )
}

@Composable
fun ItemNameText(
    name: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = name,
        style = MaterialTheme.typography.subtitle1,
        color = LookARoundTheme.colors.textPrimary,
        maxLines = maxLines,
        overflow = overflow,
        modifier = Modifier.heightIn(min = 20.dp).wrapContentHeight() then modifier
    )
}

@Composable
fun InfoItemText(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.subtitle2, color = color, modifier = modifier)
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
