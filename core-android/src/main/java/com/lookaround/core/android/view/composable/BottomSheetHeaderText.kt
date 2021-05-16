package com.lookaround.core.android.view.composable

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.view.theme.LookARoundTheme

@Composable
fun BottomSheetHeaderText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.h6,
        color = LookARoundTheme.colors.textPrimary,
        modifier =
            Modifier.heightIn(min = 56.dp)
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .wrapContentHeight()
    )
}
