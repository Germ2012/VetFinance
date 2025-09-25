// ruta: app/src/main/java/com/example/vetfinance/ui/theme/Theme.kt

package com.example.vetfinance.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.vetfinance.ui.theme.GreenContainer
import com.example.vetfinance.ui.theme.GreenPrimary
import com.example.vetfinance.ui.theme.GreenSecondary
import com.example.vetfinance.ui.theme.OnPrimaryTitle
import com.example.vetfinance.ui.theme.TextPrimary
import com.example.vetfinance.ui.theme.Typography

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = OnPrimaryTitle,
    primaryContainer = GreenContainer,
    secondary = GreenSecondary,
    onSecondary = Color.Black,
    background = Color(0xFFFBFEF9),
    surface = Color(0xFFFBFEF9),
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun VetFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.setStatusBarColor(colorScheme.primary.toArgb())
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}