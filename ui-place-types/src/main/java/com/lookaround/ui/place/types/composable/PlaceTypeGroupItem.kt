package com.lookaround.ui.place.types.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.lookaround.core.android.view.composable.ItemNameText
import com.lookaround.core.android.view.composable.LookARoundCard
import com.lookaround.core.android.view.theme.Ocean0
import com.lookaround.core.android.view.theme.Ocean2
import com.lookaround.core.model.IPlaceType
import com.lookaround.ui.place.types.model.PlaceType
import com.lookaround.ui.place.types.model.PlaceTypeGroup

@ExperimentalCoilApi
@Composable
internal fun PlaceTypeGroupItem(
    group: PlaceTypeGroup,
    columns: Int,
    modifier: Modifier = Modifier,
    onClick: (IPlaceType) -> Unit = {}
) {
    Column(modifier) {
        PlaceTypeGroupHeader(group)
        group.placeTypes.chunked(columns).forEach { chunk ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(all = 5.dp).wrapContentHeight()
            ) {
                chunk.forEach { placeType ->
                    PlaceType(
                        placeType = placeType,
                        modifier = Modifier.weight(1f, fill = false),
                        onClick = onClick
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PlaceTypeGroupHeader(group: PlaceTypeGroup) {
    LookARoundCard(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp).wrapContentWidth()
    ) {
        Text(
            group.name,
            style = MaterialTheme.typography.subtitle2,
            color = Color.Blue,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp).wrapContentWidth()
        )
    }
}

private val placeTypeShape = RoundedCornerShape(20.dp)

@ExperimentalCoilApi
@Composable
private fun PlaceType(
    placeType: PlaceType,
    modifier: Modifier = Modifier,
    onClick: (IPlaceType) -> Unit = {}
) {
    Box(
        modifier =
            Modifier.clip(placeTypeShape)
                .background(
                    brush = Brush.horizontalGradient(colors = listOf(Ocean2, Ocean0)),
                    shape = placeTypeShape,
                    alpha = if (true) .95f else .55f, // TODO: opaque/transparent logic
                )
                .clickable { onClick(placeType.wrapped) }
                .then(modifier),
        propagateMinConstraints = true
    ) {
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
