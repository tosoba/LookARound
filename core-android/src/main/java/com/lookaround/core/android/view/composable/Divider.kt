package com.lookaround.core.android.view.composable

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.view.theme.LookARoundTheme

@Composable
fun LookARoundDivider(
    modifier: Modifier = Modifier,
    color: Color = LookARoundTheme.colors.uiBorder.copy(alpha = DividerAlpha),
    thickness: Dp = 1.dp,
    startIndent: Dp = 0.dp
) {
    Divider(modifier = modifier, color = color, thickness = thickness, startIndent = startIndent)
}

private const val DividerAlpha = 0.12f
