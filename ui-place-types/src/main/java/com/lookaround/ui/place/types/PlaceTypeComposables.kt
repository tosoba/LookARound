package com.lookaround.ui.place.types

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.lookaround.core.android.view.composable.Surface
import com.lookaround.core.android.view.composable.VerticalGrid
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.place.types.model.PlaceType
import com.lookaround.ui.place.types.model.PlaceTypeGroup
import dev.chrisbanes.accompanist.coil.CoilImage
import kotlin.math.max

@Composable
fun PlaceTypes(placeTypes: List<PlaceTypeGroup>) {
    LazyColumn {
        itemsIndexed(placeTypes) { index, collection -> PlaceTypeGroup(collection, index) }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PlaceTypeImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp
) {
    Surface(
        color = Color.LightGray,
        elevation = elevation,
        shape = CircleShape,
        modifier = modifier
    ) {
        CoilImage(
            data = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PlaceTypeGroup(
    collection: PlaceTypeGroup,
    index: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = collection.name,
            style = MaterialTheme.typography.h6,
            color = LookARoundTheme.colors.textPrimary,
            modifier =
                Modifier.heightIn(min = 56.dp)
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .wrapContentHeight()
        )
        VerticalGrid(Modifier.padding(horizontal = 16.dp)) {
            val gradient =
                when (index % 2) {
                    0 -> LookARoundTheme.colors.gradient2_2
                    else -> LookARoundTheme.colors.gradient3_2
                }
            collection.placeTypes.forEach { category ->
                PlaceType(
                    category = category,
                    gradient = gradient,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

private val MinImageSize = 134.dp
private val CategoryShape = RoundedCornerShape(10.dp)
private const val CategoryTextProportion = 0.55f

@Composable
private fun PlaceType(
    category: PlaceType,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier =
            modifier
                .aspectRatio(1.45f)
                .shadow(elevation = 3.dp, shape = CategoryShape)
                .clip(CategoryShape)
                .background(Brush.horizontalGradient(gradient))
                .clickable {},
        content = {
            Text(
                text = category.name,
                style = MaterialTheme.typography.subtitle1,
                color = LookARoundTheme.colors.textSecondary,
                modifier = Modifier.padding(4.dp).padding(start = 8.dp)
            )
            PlaceTypeImage(
                imageUrl = category.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    ) { measurables, constraints ->
        // Text given a set proportion of width (which is determined by the aspect ratio)
        val textWidth = (constraints.maxWidth * CategoryTextProportion).toInt()
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

@Preview("Category")
@Composable
private fun PlaceTypePreview() {
    LookARoundTheme {
        PlaceType(
            category = PlaceType(name = "Desserts", imageUrl = ""),
            gradient = LookARoundTheme.colors.gradient3_2
        )
    }
}

@Preview("Category • Dark")
@Composable
private fun PlaceTypeDarkPreview() {
    LookARoundTheme(darkTheme = true) {
        PlaceType(
            category = PlaceType(name = "Desserts", imageUrl = ""),
            gradient = LookARoundTheme.colors.gradient3_2
        )
    }
}