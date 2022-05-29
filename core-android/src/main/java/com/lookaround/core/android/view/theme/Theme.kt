package com.lookaround.core.android.view.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.lookaround.core.android.R
import com.lookaround.core.android.ext.darkMode
import com.lookaround.core.android.view.LocalSysUiController
import kotlinx.coroutines.flow.filterNotNull

private val LightColorPalette =
    LookARoundColors(
        brand = Shadow5,
        uiBackground = Neutral0,
        uiBorder = Neutral4,
        uiFloated = FunctionalGrey,
        textSecondary = Neutral7,
        textHelp = Neutral6,
        textInteractive = Neutral0,
        textLink = Ocean11,
        iconSecondary = Neutral7,
        iconInteractive = Neutral0,
        iconInteractiveInactive = Neutral1,
        error = FunctionalRed,
        gradient6_1 = listOf(Shadow4, Ocean3, Shadow2, Ocean3, Shadow4),
        gradient6_2 = listOf(Rose4, Lavender3, Rose2, Lavender3, Rose4),
        gradient3_1 = listOf(Shadow2, Ocean3, Shadow4),
        gradient3_2 = listOf(Rose2, Lavender3, Rose4),
        gradient2_1 = listOf(Shadow4, Shadow11),
        gradient2_2 = listOf(Ocean3, Shadow3),
        isDark = false
    )

private val DarkColorPalette =
    LookARoundColors(
        brand = Shadow1,
        uiBackground = Neutral8,
        uiBorder = Neutral3,
        uiFloated = FunctionalDarkGrey,
        textPrimary = Shadow1,
        textSecondary = Neutral0,
        textHelp = Neutral1,
        textInteractive = Neutral7,
        textLink = Ocean2,
        iconPrimary = Shadow1,
        iconSecondary = Neutral0,
        iconInteractive = Neutral7,
        iconInteractiveInactive = Neutral6,
        error = FunctionalRedDark,
        gradient6_1 = listOf(Shadow5, Ocean7, Shadow9, Ocean7, Shadow5),
        gradient6_2 = listOf(Rose11, Lavender7, Rose8, Lavender7, Rose11),
        gradient3_1 = listOf(Shadow9, Ocean7, Shadow5),
        gradient3_2 = listOf(Rose8, Lavender7, Rose11),
        gradient2_1 = listOf(Ocean3, Shadow3),
        gradient2_2 = listOf(Ocean7, Shadow7),
        isDark = true
    )

val Context.colorPalette: LookARoundColors
    get() = if (darkMode) DarkColorPalette else LightColorPalette

@Composable
fun LookARoundTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val preferences =
        FlowSharedPreferences(PreferenceManager.getDefaultSharedPreferences(LocalContext.current))
    val themePreferenceKey = stringResource(id = R.string.preference_theme_key)
    val systemTheme = stringResource(id = R.string.preference_theme_system_value)
    val themePreferenceFlow = remember {
        preferences.getString(themePreferenceKey, systemTheme).asFlow().filterNotNull()
    }
    val themePreferenceState = themePreferenceFlow.collectAsState(initial = systemTheme)
    val userDarkTheme =
        darkTheme
            ?: when (themePreferenceState.value) {
                stringResource(id = R.string.preference_theme_light_value) -> false
                stringResource(id = R.string.preference_theme_dark_value) -> true
                systemTheme -> isSystemInDarkTheme()
                else -> throw IllegalStateException()
            }
    val colors = if (userDarkTheme) DarkColorPalette else LightColorPalette

    val sysUiController = LocalSysUiController.current
    SideEffect {
        sysUiController.setSystemBarsColor(
            color = colors.uiBackground.copy(alpha = AlphaNearOpaque)
        )
    }

    ProvideLookARoundColors(colors) {
        MaterialTheme(
            colors = debugColors(userDarkTheme),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

object LookARoundTheme {
    val colors: LookARoundColors
        @Composable get() = LocalLookARoundColors.current
}

@Stable
class LookARoundColors(
    gradient6_1: List<Color>,
    gradient6_2: List<Color>,
    gradient3_1: List<Color>,
    gradient3_2: List<Color>,
    gradient2_1: List<Color>,
    gradient2_2: List<Color>,
    brand: Color,
    uiBackground: Color,
    uiBorder: Color,
    uiFloated: Color,
    interactivePrimary: List<Color> = gradient2_1,
    interactiveSecondary: List<Color> = gradient2_2,
    interactiveMask: List<Color> = gradient6_1,
    textPrimary: Color = brand,
    textSecondary: Color,
    textHelp: Color,
    textInteractive: Color,
    textLink: Color,
    iconPrimary: Color = brand,
    iconSecondary: Color,
    iconInteractive: Color,
    iconInteractiveInactive: Color,
    error: Color,
    notificationBadge: Color = error,
    isDark: Boolean
) {
    var gradient6_1 by mutableStateOf(gradient6_1)
        private set
    var gradient6_2 by mutableStateOf(gradient6_2)
        private set
    var gradient3_1 by mutableStateOf(gradient3_1)
        private set
    var gradient3_2 by mutableStateOf(gradient3_2)
        private set
    var gradient2_1 by mutableStateOf(gradient2_1)
        private set
    var gradient2_2 by mutableStateOf(gradient2_2)
        private set
    var brand by mutableStateOf(brand)
        private set
    var uiBackground by mutableStateOf(uiBackground)
        private set
    var uiBorder by mutableStateOf(uiBorder)
        private set
    var uiFloated by mutableStateOf(uiFloated)
        private set
    var interactivePrimary by mutableStateOf(interactivePrimary)
        private set
    var interactiveSecondary by mutableStateOf(interactiveSecondary)
        private set
    var interactiveMask by mutableStateOf(interactiveMask)
        private set
    var textPrimary by mutableStateOf(textPrimary)
        private set
    var textSecondary by mutableStateOf(textSecondary)
        private set
    var textHelp by mutableStateOf(textHelp)
        private set
    var textInteractive by mutableStateOf(textInteractive)
        private set
    var textLink by mutableStateOf(textLink)
        private set
    var iconPrimary by mutableStateOf(iconPrimary)
        private set
    var iconSecondary by mutableStateOf(iconSecondary)
        private set
    var iconInteractive by mutableStateOf(iconInteractive)
        private set
    var iconInteractiveInactive by mutableStateOf(iconInteractiveInactive)
        private set
    var error by mutableStateOf(error)
        private set
    var notificationBadge by mutableStateOf(notificationBadge)
        private set
    var isDark by mutableStateOf(isDark)
        private set

    fun update(other: LookARoundColors) {
        gradient6_1 = other.gradient6_1
        gradient6_2 = other.gradient6_2
        gradient3_1 = other.gradient3_1
        gradient3_2 = other.gradient3_2
        gradient2_1 = other.gradient2_1
        gradient2_2 = other.gradient2_2
        brand = other.brand
        uiBackground = other.uiBackground
        uiBorder = other.uiBorder
        uiFloated = other.uiFloated
        interactivePrimary = other.interactivePrimary
        interactiveSecondary = other.interactiveSecondary
        interactiveMask = other.interactiveMask
        textPrimary = other.textPrimary
        textSecondary = other.textSecondary
        textHelp = other.textHelp
        textInteractive = other.textInteractive
        textLink = other.textLink
        iconPrimary = other.iconPrimary
        iconSecondary = other.iconSecondary
        iconInteractive = other.iconInteractive
        iconInteractiveInactive = other.iconInteractiveInactive
        error = other.error
        notificationBadge = other.notificationBadge
        isDark = other.isDark
    }
}

@Composable
fun ProvideLookARoundColors(colors: LookARoundColors, content: @Composable () -> Unit) {
    val colorPalette = remember { colors }
    colorPalette.update(colors)
    CompositionLocalProvider(LocalLookARoundColors provides colorPalette, content = content)
}

private val LocalLookARoundColors =
    staticCompositionLocalOf<LookARoundColors> { error("No LookARoundColors provided") }

/**
 * A Material [Colors] implementation which sets all colors to [debugColor] to discourage usage of
 * [MaterialTheme.colors] in preference to [LookARoundTheme.colors].
 */
fun debugColors(darkTheme: Boolean, debugColor: Color = Color.Magenta) =
    Colors(
        primary = debugColor,
        primaryVariant = debugColor,
        secondary = debugColor,
        secondaryVariant = debugColor,
        background = debugColor,
        surface = debugColor,
        error = debugColor,
        onPrimary = debugColor,
        onSecondary = debugColor,
        onBackground = debugColor,
        onSurface = debugColor,
        onError = debugColor,
        isLight = !darkTheme
    )
