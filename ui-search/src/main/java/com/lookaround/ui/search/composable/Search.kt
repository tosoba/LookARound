package com.lookaround.ui.search.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    LookARoundSurface(modifier = modifier.fillMaxSize()) {
        Column {
            Spacer(modifier = Modifier.statusBarsPadding())
            SearchBar(
                query = state.query,
                onQueryChange = { state.query = it },
                searchFocused = state.focused,
                onSearchFocusChange = { state.focused = it },
                onClearQuery = { state.query = TextFieldValue("") },
                searching = state.searching
            )
            LookARoundDivider()
            LaunchedEffect(state.query.text) {
                state.searching = true
                // TODO: trigger search in VM
                state.searching = false
            }
        }
    }
}

@Composable
private fun rememberSearchState(
    query: TextFieldValue = TextFieldValue(""),
    focused: Boolean = false,
    searching: Boolean = false,
): SearchState = remember {
    SearchState(
        query = query,
        focused = focused,
        searching = searching,
    )
}

@Stable
class SearchState(
    query: TextFieldValue,
    focused: Boolean,
    searching: Boolean,
) {
    var query by mutableStateOf(query)
    var focused by mutableStateOf(focused)
    var searching by mutableStateOf(searching)
}

@Composable
private fun SearchBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    searchFocused: Boolean,
    onSearchFocusChange: (Boolean) -> Unit,
    onClearQuery: () -> Unit,
    searching: Boolean,
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
            if (query.text.isEmpty()) {
                SearchHint()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().wrapContentHeight()
            ) {
                if (searchFocused) {
                    IconButton(onClick = onClearQuery) {
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
                if (searching) {
                    CircularProgressIndicator(
                        color = LookARoundTheme.colors.iconPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp).size(36.dp)
                    )
                } else {
                    Spacer(Modifier.width(IconSize)) // balance arrow icon
                }
            }
        }
    }
}

private val IconSize = 48.dp

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
                onClearQuery = {},
                searching = false
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
                onClearQuery = {},
                searching = false
            )
        }
    }
}
