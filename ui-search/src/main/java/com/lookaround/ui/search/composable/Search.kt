package com.lookaround.ui.search.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lookaround.core.android.view.composable.LookARoundDivider
import com.lookaround.core.android.view.composable.LookARoundSurface
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.search.R
import dev.chrisbanes.accompanist.insets.statusBarsPadding

@Composable
fun Search(modifier: Modifier = Modifier, state: SearchState = rememberSearchState()) {
    LookARoundSurface(modifier = modifier.wrapContentHeight()) {
        Column {
            Spacer(modifier = Modifier.statusBarsPadding())
            SearchBar(
                query = state.query,
                onQueryChange = { state.query = it },
                searchFocused = state.focused,
                onSearchFocusChange = { state.focused = it },
                onBackArrowClicked = { state.focused = false },
                onClearQueryClicked = { state.query = TextFieldValue("") }
            )
            LookARoundDivider()
            LaunchedEffect(state.query.text) {
                if (state.query.text.isBlank()) return@LaunchedEffect
                // TODO: trigger search in VM
            }
        }
    }
}

@Composable
private fun rememberSearchState(
    query: TextFieldValue = TextFieldValue(""),
    focused: Boolean = false,
): SearchState = remember { SearchState(query = query, focused = focused) }

@Stable
class SearchState(query: TextFieldValue, focused: Boolean) {
    var query by mutableStateOf(query)
    var focused by mutableStateOf(focused)
}

@Composable
private fun SearchBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    searchFocused: Boolean,
    onSearchFocusChange: (Boolean) -> Unit,
    onBackArrowClicked: () -> Unit,
    onClearQueryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    LookARoundSurface(
        color = LookARoundTheme.colors.uiFloated,
        contentColor = LookARoundTheme.colors.textSecondary,
        shape = MaterialTheme.shapes.small,
        modifier =
            modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (query.text.isEmpty()) SearchHint()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().wrapContentHeight()
            ) {
                if (searchFocused) {
                    IconButton(onClick = onBackArrowClicked) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            tint = LookARoundTheme.colors.iconPrimary,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier =
                        Modifier.weight(1f).onFocusChanged { onSearchFocusChange(it.isFocused) }
                )
                when {
                    query.text.isNotEmpty() ->
                        IconButton(onClick = onClearQueryClicked) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                tint = LookARoundTheme.colors.iconPrimary,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    else -> Spacer(Modifier.width(48.dp)) // balance arrow icon
                }
            }
        }
    }
}

@Composable
private fun SearchHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize().wrapContentSize()
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            tint = LookARoundTheme.colors.textHelp,
            contentDescription = stringResource(R.string.perform_search)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.search_3_dots), color = LookARoundTheme.colors.textHelp)
    }
}

@Preview("Search Bar")
@Composable
private fun SearchBarPreview() {
    LookARoundTheme {
        LookARoundSurface {
            SearchBar(
                query = TextFieldValue(""),
                onQueryChange = {},
                searchFocused = false,
                onSearchFocusChange = {},
                onBackArrowClicked = {},
                onClearQueryClicked = {}
            )
        }
    }
}

@Preview("Search Bar • Dark")
@Composable
private fun SearchBarDarkPreview() {
    LookARoundTheme(darkTheme = true) {
        LookARoundSurface {
            SearchBar(
                query = TextFieldValue(""),
                onQueryChange = {},
                searchFocused = false,
                onSearchFocusChange = {},
                onBackArrowClicked = {},
                onClearQueryClicked = {}
            )
        }
    }
}
