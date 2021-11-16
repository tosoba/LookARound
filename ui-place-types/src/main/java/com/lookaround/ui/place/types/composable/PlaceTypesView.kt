package com.lookaround.ui.place.types.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.lookaround.core.android.view.composable.LookARoundCard
import com.lookaround.core.android.view.composable.LookARoundSurface
import com.lookaround.core.android.view.composable.VerticalGrid
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.android.model.Amenity
import com.lookaround.core.model.IPlaceType
import com.lookaround.ui.place.types.model.PlaceType
import com.lookaround.ui.place.types.model.PlaceTypeGroup
import kotlin.math.max

@Composable
internal fun PlaceTypeGroupItem(
    group: PlaceTypeGroup,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: (IPlaceType) -> Unit = {}
) {
    Column(modifier) {
        PlaceTypeGroupHeader(group)
        VerticalGrid(Modifier.padding(horizontal = 16.dp)) {
            val gradient =
                when (index % 2) {
                    0 -> LookARoundTheme.colors.gradient2_2
                    else -> LookARoundTheme.colors.gradient3_2
                }
            group.placeTypes.forEach { placeType ->
                PlaceType(
                    placeType = placeType,
                    gradient = gradient,
                    modifier = Modifier.padding(8.dp),
                    onClick = onClick
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PlaceTypeImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp
) {
    LookARoundSurface(
        color = Color.LightGray,
        elevation = elevation,
        shape = CircleShape,
        modifier = modifier
    ) {
        Image(
            painter = rememberImagePainter(imageUrl),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
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

private val MinImageSize = 134.dp
private val PlaceTypeShape = RoundedCornerShape(10.dp)
private const val PlaceTypeTextProportion = 0.55f

@Composable
private fun PlaceType(
    placeType: PlaceType,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: (IPlaceType) -> Unit = {}
) {
    Layout(
        modifier =
            modifier
                .aspectRatio(1.45f)
                .shadow(elevation = 3.dp, shape = PlaceTypeShape)
                .clip(PlaceTypeShape)
                .background(Brush.horizontalGradient(gradient))
                .clickable { onClick(placeType.wrapped) },
        content = {
            Text(
                text = placeType.wrapped.label,
                style = MaterialTheme.typography.subtitle1,
                color = LookARoundTheme.colors.textSecondary,
                modifier = Modifier.padding(4.dp).padding(start = 8.dp)
            )
            PlaceTypeImage(
                imageUrl = placeType.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    ) { measurables, constraints ->
        // Text given a set proportion of width (which is determined by the aspect ratio)
        val textWidth = (constraints.maxWidth * PlaceTypeTextProportion).toInt()
        val textPlaceable = measurables[0].measure(Constraints.fixedWidth(textWidth))

        // Image is sized to the larger of height of item, or a minimum value
        // i.e. may appear larger than item (but clipped to the item bounds)
        val imageSize = max(MinImageSize.roundToPx(), constraints.maxHeight)
        val imagePlaceable = measurables[1].measure(Constraints.fixed(imageSize, imageSize))
        layout(width = constraints.maxWidth, height = constraints.minHeight) {
            textPlaceable.place(
                x = 0,
                y = (constraints.maxHeight - textPlaceable.height) / 2 // centered
            )
            imagePlaceable.place(
                // image is placed to end of text i.e. will overflow to the end (but be clipped)
                x = textWidth,
                y = (constraints.maxHeight - imagePlaceable.height) / 2 // centered
            )
        }
    }
}

@Preview("PlaceType")
@Composable
private fun PlaceTypePreview() {
    LookARoundTheme {
        PlaceType(
            placeType = PlaceType(wrapped = Amenity.BANK, imageUrl = ""),
            gradient = LookARoundTheme.colors.gradient3_2
        )
    }
}

@Preview("PlaceType • Dark")
@Composable
private fun PlaceTypeDarkPreview() {
    LookARoundTheme(darkTheme = true) {
        PlaceType(
            placeType = PlaceType(wrapped = Amenity.BANK, imageUrl = ""),
            gradient = LookARoundTheme.colors.gradient3_2
        )
    }
}
