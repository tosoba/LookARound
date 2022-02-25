package com.lookaround.ui.place.categories.composable

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.view.composable.ItemNameText
import com.lookaround.ui.place.categories.model.PlaceCategory
import com.lookaround.ui.place.categories.model.PlaceType

@Composable
internal fun PlaceCategoryHeader(category: PlaceCategory, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.padding(horizontal = 15.dp).wrapContentWidth().then(modifier),
        propagateMinConstraints = true
    ) {
        Text(
            category.name,
            style = MaterialTheme.typography.h6,
            color = Color.Blue,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

internal val placeTypeShape = RoundedCornerShape(20.dp)

@Composable
internal fun PlaceType(placeType: PlaceType, modifier: Modifier = Modifier) {
    Box(modifier = modifier, propagateMinConstraints = true) {
        Column(modifier = Modifier.padding(10.dp)) {
            Card(elevation = 3.dp, shape = placeTypeShape, modifier = Modifier.aspectRatio(1.5f)) {
                Image(
                    painter = painterResource(id = placeType.drawableId),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
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
