package app.ficha.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.ficha.R
import app.ficha.data.Settings
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant

/** As 10 cores do PWA; a versão viva (do tema escuro) é a cor de origem da paleta. */
class Accent(val id: String, val name: String, val seed: Long)

val ACCENTS = listOf(
    Accent("limao", "Limão", 0xFFC8F250),
    Accent("verde", "Verde", 0xFF34D399),
    Accent("turquesa", "Turquesa", 0xFF2DD4BF),
    Accent("azul", "Azul", 0xFF4DA3FF),
    Accent("roxo", "Roxo", 0xFFA78BFA),
    Accent("rosa", "Rosa", 0xFFFF6FAE),
    Accent("vermelho", "Vermelho", 0xFFFF5A52),
    Accent("laranja", "Laranja", 0xFFFF9F43),
    Accent("amarelo", "Amarelo", 0xFFFFD43B),
    Accent("mono", "Grafite", 0xFF8A8F98),
)

val THEMES = listOf("auto" to "Sistema", "light" to "Claro", "dark" to "Escuro", "black" to "Preto")
val PALETTES = listOf("tonal" to "Tonal", "vibrant" to "Vibrante", "expressive" to "Expressiva")

fun accentOf(id: String) = ACCENTS.find { it.id == id } ?: ACCENTS[0]

/** Cores extras do app (recordes, medalhas) que não existem no Material. */
@Immutable
class FichaColors(val gold: Color, val goldContainer: Color, val onGoldContainer: Color, val success: Color)

val LocalFichaColors = staticCompositionLocalOf { FichaColors(Color(0xFFE5B315), Color(0xFF4A3B00), Color(0xFFFFE07A), Color(0xFF34D399)) }

/** Paleta tonal do Material 3 (especificação de 2025) gerada a partir da cor escolhida. */
fun schemeFromSeed(seed: Long, dark: Boolean, palette: String, mono: Boolean): ColorScheme {
    val hct = Hct.fromInt(seed.toInt())
    val spec = ColorSpec.SpecVersion.SPEC_2025
    val s: DynamicScheme = when {
        mono -> SchemeMonochrome(hct, dark, 0.0, spec)
        palette == "tonal" -> SchemeTonalSpot(hct, dark, 0.0, spec)
        palette == "expressive" -> SchemeExpressive(hct, dark, 0.0, spec)
        else -> SchemeVibrant(hct, dark, 0.0, spec)
    }
    val m = MaterialDynamicColors()
    fun c(d: DynamicColor) = Color(d.getArgb(s))
    // Todas as cores vêm do esquema dinâmico (claro ou escuro); os valores padrão não são usados
    return darkColorScheme(
        primary = c(m.primary()),
        onPrimary = c(m.onPrimary()),
        primaryContainer = c(m.primaryContainer()),
        onPrimaryContainer = c(m.onPrimaryContainer()),
        inversePrimary = c(m.inversePrimary()),
        secondary = c(m.secondary()),
        onSecondary = c(m.onSecondary()),
        secondaryContainer = c(m.secondaryContainer()),
        onSecondaryContainer = c(m.onSecondaryContainer()),
        tertiary = c(m.tertiary()),
        onTertiary = c(m.onTertiary()),
        tertiaryContainer = c(m.tertiaryContainer()),
        onTertiaryContainer = c(m.onTertiaryContainer()),
        background = c(m.background()),
        onBackground = c(m.onBackground()),
        surface = c(m.surface()),
        onSurface = c(m.onSurface()),
        surfaceVariant = c(m.surfaceVariant()),
        onSurfaceVariant = c(m.onSurfaceVariant()),
        inverseSurface = c(m.inverseSurface()),
        inverseOnSurface = c(m.inverseOnSurface()),
        error = c(m.error()),
        onError = c(m.onError()),
        errorContainer = c(m.errorContainer()),
        onErrorContainer = c(m.onErrorContainer()),
        outline = c(m.outline()),
        outlineVariant = c(m.outlineVariant()),
        scrim = c(m.scrim()),
        surfaceBright = c(m.surfaceBright()),
        surfaceContainer = c(m.surfaceContainer()),
        surfaceContainerHigh = c(m.surfaceContainerHigh()),
        surfaceContainerHighest = c(m.surfaceContainerHighest()),
        surfaceContainerLow = c(m.surfaceContainerLow()),
        surfaceContainerLowest = c(m.surfaceContainerLowest()),
        surfaceDim = c(m.surfaceDim()),
        primaryFixed = c(m.primaryFixed()),
        primaryFixedDim = c(m.primaryFixedDim()),
        onPrimaryFixed = c(m.onPrimaryFixed()),
        onPrimaryFixedVariant = c(m.onPrimaryFixedVariant()),
        secondaryFixed = c(m.secondaryFixed()),
        secondaryFixedDim = c(m.secondaryFixedDim()),
        onSecondaryFixed = c(m.onSecondaryFixed()),
        onSecondaryFixedVariant = c(m.onSecondaryFixedVariant()),
        tertiaryFixed = c(m.tertiaryFixed()),
        tertiaryFixedDim = c(m.tertiaryFixedDim()),
        onTertiaryFixed = c(m.onTertiaryFixed()),
        onTertiaryFixedVariant = c(m.onTertiaryFixedVariant()),
        surfaceTint = c(m.primary()),
    )
}

/** Tema Preto (OLED): fundo preto e os contêineres um pouco mais escuros. */
private fun ColorScheme.amoled(): ColorScheme {
    val k = 0.45f
    return copy(
        background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = lerp(surfaceContainerLow, Color.Black, k),
        surfaceContainer = lerp(surfaceContainer, Color.Black, k),
        surfaceContainerHigh = lerp(surfaceContainerHigh, Color.Black, k),
        surfaceContainerHighest = lerp(surfaceContainerHighest, Color.Black, k),
    )
}

val GoogleSansFlex = FontFamily(
    listOf(400 to FontWeight.Normal, 500 to FontWeight.Medium, 600 to FontWeight.SemiBold, 700 to FontWeight.Bold).map { (w, fw) ->
        Font(R.font.google_sans_flex, fw, variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

private fun TextStyle.sans() = copy(fontFamily = GoogleSansFlex)

private val AppTypography: Typography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.sans(), displayMedium = t.displayMedium.sans(), displaySmall = t.displaySmall.sans(),
        headlineLarge = t.headlineLarge.sans(), headlineMedium = t.headlineMedium.sans(), headlineSmall = t.headlineSmall.sans(),
        titleLarge = t.titleLarge.sans(), titleMedium = t.titleMedium.sans(), titleSmall = t.titleSmall.sans(),
        bodyLarge = t.bodyLarge.sans(), bodyMedium = t.bodyMedium.sans(), bodySmall = t.bodySmall.sans(),
        labelLarge = t.labelLarge.sans(), labelMedium = t.labelMedium.sans(), labelSmall = t.labelSmall.sans(),
        displayLargeEmphasized = t.displayLargeEmphasized.sans(), displayMediumEmphasized = t.displayMediumEmphasized.sans(),
        displaySmallEmphasized = t.displaySmallEmphasized.sans(), headlineLargeEmphasized = t.headlineLargeEmphasized.sans(),
        headlineMediumEmphasized = t.headlineMediumEmphasized.sans(), headlineSmallEmphasized = t.headlineSmallEmphasized.sans(),
        titleLargeEmphasized = t.titleLargeEmphasized.sans(), titleMediumEmphasized = t.titleMediumEmphasized.sans(),
        titleSmallEmphasized = t.titleSmallEmphasized.sans(), bodyLargeEmphasized = t.bodyLargeEmphasized.sans(),
        bodyMediumEmphasized = t.bodyMediumEmphasized.sans(), bodySmallEmphasized = t.bodySmallEmphasized.sans(),
        labelLargeEmphasized = t.labelLargeEmphasized.sans(), labelMediumEmphasized = t.labelMediumEmphasized.sans(),
        labelSmallEmphasized = t.labelSmallEmphasized.sans(),
    )
}

@Composable
fun isDarkTheme(settings: Settings): Boolean = when (settings.theme) {
    "light" -> false
    "dark", "black" -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun FichaTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings)
    val context = LocalContext.current
    val dynamic = settings.androidDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val accent = accentOf(settings.accent)
    val black = settings.theme == "black"
    val scheme = remember(dark, dynamic, accent.id, settings.androidPalette, black) {
        val base = if (dynamic) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            schemeFromSeed(accent.seed, dark, settings.androidPalette, accent.id == "mono")
        }
        if (black && dark) base.amoled() else base
    }
    val extra = remember(dark) {
        if (dark) FichaColors(Color(0xFFFFD54F), Color(0xFF4A3B00), Color(0xFFFFE07A), Color(0xFF4ADE9F))
        else FichaColors(Color(0xFF9A6B00), Color(0xFFFFE08A), Color(0xFF3A2A00), Color(0xFF15803D))
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalFichaColors provides extra) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            typography = AppTypography,
            content = content,
        )
    }
}

object Fx {
    val colors: FichaColors @Composable get() = LocalFichaColors.current
    val scheme: ColorScheme @Composable get() = MaterialTheme.colorScheme
}
