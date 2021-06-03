package com.lookaround.core.android.view.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.R
import com.lookaround.core.android.view.theme.LookARoundTheme

@Composable
fun BottomSheetHeaderText(text: String) {
    LookARoundCard(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp).heightIn(45.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Icon(
                imageVector = Icons.Filled.ExpandLess,
                contentDescription = stringResource(R.string.expand)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.h6,
                color = LookARoundTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp).fillMaxHeight()
            )
        }
    }
}
