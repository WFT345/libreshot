package app.libreshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Ios {
    val Background = Color(0xFF1C1C1E)
    val Surface = Color(0xFF2C2C2E)
    val Separator = Color(0xFF3A3A3C)
    val PrimaryText = Color.White
    val SecondaryText = Color(0xFF8E8E93)
    val Accent = Color(0xFF0A84FF)
    val Destructive = Color(0xFFFF453A)
}

private val Scheme = darkColorScheme(
    primary = Ios.Accent,
    background = Ios.Background,
    surface = Ios.Surface,
    onBackground = Ios.PrimaryText,
    onSurface = Ios.PrimaryText,
    onSurfaceVariant = Ios.SecondaryText,
    error = Ios.Destructive,
    outline = Ios.Separator,
)

@Composable
fun LibreShotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
