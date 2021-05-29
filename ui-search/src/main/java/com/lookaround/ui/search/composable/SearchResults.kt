package com.lookaround.ui.search.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.model.Point
import com.lookaround.core.android.view.theme.LookARoundTheme

@Composable
internal fun SearchResults(points: List<Point>, modifier: Modifier = Modifier) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(points) { item ->
            // TODO: distance away from current location?
            Card(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(5.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.subtitle1,
                        color = LookARoundTheme.colors.textPrimary,
                        modifier = Modifier.heightIn(min = 20.dp).wrapContentHeight()
                    )
                }
            }
        }
    }
}
