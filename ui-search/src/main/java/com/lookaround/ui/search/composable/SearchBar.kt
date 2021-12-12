package com.lookaround.ui.search.composable

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.statusBarsPadding
import com.lookaround.core.android.view.composable.LookARoundSurface
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.search.R

@Composable
fun SearchBar(
    query: String,
    searchFocused: Boolean,
    modifier: Modifier = Modifier,
    onSearchFocusChange: (Boolean) -> Unit = {},
    onTextValueChange: (TextFieldValue) -> Unit = {}
) {
    var textValue by remember { mutableStateOf(TextFieldValue(query)) }
    val focusManager = LocalFocusManager.current
    val onBackPressedDispatcher =
        (LocalLifecycleOwner.current as ComponentActivity).onBackPressedDispatcher
    val onBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                focusManager.clearFocus()
            }
        }
    val focusRequester = remember(::FocusRequester)
    LookARoundSurface(color = Color.Transparent, modifier = modifier.wrapContentHeight()) {
        Column {
            Spacer(modifier = Modifier.statusBarsPadding())
            SearchBar(
                textValue = textValue,
                searchFocused = searchFocused,
                onTextValueChange = { value ->
                    textValue = value
                    onTextValueChange(value)
                },
                onSearchFocusChange = { focused ->
                    onSearchFocusChange(focused)
                    if (focused) onBackPressedDispatcher.addCallback(onBackPressedCallback)
                    else onBackPressedCallback.remove()
                },
                onBackArrowClicked = focusManager::clearFocus,
                onClearQueryClicked = {
                    textValue = TextFieldValue("")
                    focusRequester.requestFocus()
                },
                focusRequester = focusRequester,
            )
        }
        if (searchFocused) LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun SearchBar(
    textValue: TextFieldValue,
    searchFocused: Boolean,
    onTextValueChange: (TextFieldValue) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onBackArrowClicked: () -> Unit,
    onClearQueryClicked: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    LookARoundSurface(
        color = LookARoundTheme.colors.uiFloated,
        contentColor = LookARoundTheme.colors.textSecondary,
        shape = MaterialTheme.shapes.small,
        modifier =
            modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (textValue.text.isEmpty()) SearchHint()
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
                    value = textValue,
                    onValueChange = onTextValueChange,
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    singleLine = true,
                    modifier =
                        Modifier.weight(1f)
                            .padding(horizontal = if (searchFocused) 5.dp else 10.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { onSearchFocusChange(it.isFocused) }
                )
                when {
                    textValue.text.isNotEmpty() ->
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
                textValue = TextFieldValue(""),
                onTextValueChange = {},
                searchFocused = false,
                onSearchFocusChange = {},
                onBackArrowClicked = {},
                onClearQueryClicked = {},
                focusRequester = FocusRequester(),
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
                textValue = TextFieldValue(""),
                onTextValueChange = {},
                searchFocused = false,
                onSearchFocusChange = {},
                onBackArrowClicked = {},
                onClearQueryClicked = {},
                focusRequester = FocusRequester(),
            )
        }
    }
}
