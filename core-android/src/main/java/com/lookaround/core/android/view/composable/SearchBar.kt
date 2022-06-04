package com.lookaround.core.android.view.composable

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.statusBarsPadding
import com.lookaround.core.android.R
import com.lookaround.core.android.ext.darkMode
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.android.view.theme.Neutral1

@Composable
fun SearchBar(
    query: String,
    focused: Boolean,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    modifier: Modifier = Modifier,
    leadingUnfocused: @Composable () -> Unit = {},
    onSearchFocusChange: (Boolean) -> Unit = {},
    onTextFieldValueChange: (TextFieldValue) -> Unit = {}
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(query)) }
    val focusManager = LocalFocusManager.current
    val onBackPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                focusManager.clearFocus()
            }
        }
    }
    val focusRequester = remember(::FocusRequester)
    LookARoundSurface(color = Color.Transparent, modifier = modifier.wrapContentHeight()) {
        Column {
            Spacer(modifier = Modifier.statusBarsPadding())
            SearchBar(
                value = textFieldValue,
                focused = focused,
                onTextValueChange = { value ->
                    textFieldValue = value
                    onTextFieldValueChange(value)
                },
                onSearchFocusChange = { focused ->
                    onSearchFocusChange(focused)
                    if (focused) onBackPressedDispatcher.addCallback(onBackPressedCallback)
                    else onBackPressedCallback.remove()
                },
                onBackArrowClicked = focusManager::clearFocus,
                onClearQueryClicked = {
                    val clearedTextFieldValue = TextFieldValue("")
                    textFieldValue = clearedTextFieldValue
                    onTextFieldValueChange(clearedTextFieldValue)
                    focusRequester.requestFocus()
                },
                focusRequester = focusRequester,
                leadingUnfocused = leadingUnfocused,
            )
        }
        if (focused) LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun SearchBar(
    value: TextFieldValue,
    focused: Boolean,
    onTextValueChange: (TextFieldValue) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onBackArrowClicked: () -> Unit,
    onClearQueryClicked: () -> Unit,
    focusRequester: FocusRequester,
    leadingUnfocused: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    LookARoundSurface(
        color = LookARoundTheme.colors.uiFloated,
        contentColor = LookARoundTheme.colors.textSecondary,
        shape = MaterialTheme.shapes.small,
        elevation = 5.dp,
        modifier =
            modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (value.text.isEmpty()) SearchHint()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().wrapContentHeight()
            ) {
                if (focused) {
                    IconButton(onClick = onBackArrowClicked) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            tint = LookARoundTheme.colors.iconPrimary,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                } else {
                    leadingUnfocused()
                }
                BasicTextField(
                    value = value,
                    textStyle =
                        if (darkMode) TextStyle.Default.copy(color = Neutral1)
                        else TextStyle.Default,
                    cursorBrush = if (darkMode) SolidColor(Neutral1) else SolidColor(Color.Black),
                    onValueChange = onTextValueChange,
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    singleLine = true,
                    modifier =
                        Modifier.weight(1f)
                            .padding(horizontal = if (focused) 5.dp else 10.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { onSearchFocusChange(it.isFocused) }
                )
                when {
                    value.text.isNotEmpty() ->
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
                value = TextFieldValue(""),
                focused = false,
                onTextValueChange = {},
                onSearchFocusChange = {},
                onBackArrowClicked = {},
                onClearQueryClicked = {},
                focusRequester = FocusRequester(),
                leadingUnfocused = {},
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
                value = TextFieldValue(""),
                onTextValueChange = {},
                focused = false,
                onSearchFocusChange = {},
                onBackArrowClicked = {},
                onClearQueryClicked = {},
                focusRequester = FocusRequester(),
                leadingUnfocused = {},
            )
        }
    }
}
