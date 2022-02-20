package com.lookaround.ui.place.types.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.lookaround.core.android.view.composable.ItemNameText
import com.lookaround.ui.place.types.model.PlaceType
import com.lookaround.ui.place.types.model.PlaceTypeGroup

@Composable
internal fun PlaceTypeGroupHeader(group: PlaceTypeGroup, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.padding(horizontal = 15.dp).wrapContentWidth().then(modifier),
        propagateMinConstraints = true
    ) {
        Text(
            group.name,
            style = MaterialTheme.typography.subtitle2,
            color = Color.Blue,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

internal val placeTypeShape = RoundedCornerShape(20.dp)

@ExperimentalCoilApi
@Composable
internal fun PlaceType(placeType: PlaceType, modifier: Modifier = Modifier) {
    Box(modifier = modifier, propagateMinConstraints = true) {
        Column(modifier = Modifier.padding(10.dp)) {
            Card(
                elevation = 3.dp,
                shape = placeTypeShape,
                modifier = Modifier.wrapContentWidth().aspectRatio(1.5f)
            ) {
                Image(
                    painter = rememberImagePainter(placeType.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ItemNameText(
                placeType.wrapped.label,
                modifier = Modifier.padding(vertical = 5.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
