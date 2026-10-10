package app.swisszen.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.swisszen.R

/** One full set of colour tokens; [ZenLight] is the approved prototype, [ZenDark] its night twin. */
class ZenPalette(
    val bg: Color, val surface: Color, val surface2: Color, val line: Color,
    val ink: Color, val muted: Color, val faint: Color,
    val pine: Color, val pine2: Color, val pineSoft: Color, val onPine: Color,
    val ice: Color, val ice2: Color, val ice3: Color, val iceHi: Color,
    val red: Color, val track: Color, val shadow: Color,
    /** Glyph discs: centre highlight → mid → rim. */
    val halo: List<Color>,
    /** Breathing sphere gradient, lit from the upper left: highlight → rim. */
    val orb: List<Color>,
)

val ZenLight = ZenPalette(
    bg = Color(0xFFF7F4EE), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFFBF9F5), line = Color(0xFFE8E2D7),
    ink = Color(0xFF1B2621), muted = Color(0xFF6C7671), faint = Color(0xFFA0A6A2),
    pine = Color(0xFF1E4D3B), pine2 = Color(0xFF2E6A52), pineSoft = Color(0xFFE4ECE7), onPine = Color(0xFFF7F4EE),
    ice = Color(0xFFDFECF2), ice2 = Color(0xFFB9D4DF), ice3 = Color(0xFF7FA8BA), iceHi = Color(0xFFEFF5F8),
    red = Color(0xFFE30613), track = Color(0xFFD9D5CC), shadow = Color(0xFF1E4D3B),
    halo = listOf(Color.White, Color(0xFFE6F0F4), Color(0xFFC3DAE3)),
    orb = listOf(Color.White, Color(0xFFEEF5F8), Color(0xFFC9DFE8), Color(0xFF94BACA), Color(0xFF7FA8BA)),
)

// Pine flips to a light sage so it still reads as text/icons on the dark ground; fills that use it
// (buttons, chips, the toast) get dark OnPine content to match.
val ZenDark = ZenPalette(
    bg = Color(0xFF111513), surface = Color(0xFF1A201D), surface2 = Color(0xFF151A17), line = Color(0xFF2A322E),
    ink = Color(0xFFE9E6DF), muted = Color(0xFF9BA39F), faint = Color(0xFF6D7571),
    pine = Color(0xFF8FC4AA), pine2 = Color(0xFF6DAE90), pineSoft = Color(0xFF1F2B25), onPine = Color(0xFF0F1A15),
    ice = Color(0xFF1C2A30), ice2 = Color(0xFF3D5A67), ice3 = Color(0xFF7FA8BA), iceHi = Color(0xFF1F2E35),
    red = Color(0xFFE30613), track = Color(0xFF3A423E), shadow = Color.Black,
    halo = listOf(Color(0xFF2E414A), Color(0xFF223239), Color(0xFF1B282E)),
    // dimmed so the sage numbers on top stay readable
    orb = listOf(Color(0xFF557380), Color(0xFF41606C), Color(0xFF2E4650), Color(0xFF23363E), Color(0xFF1E2F36)),
)

/** Design tokens taken one-to-one from the approved prototype; they follow the active palette. */
object Zen {
    // Snapshot state, so composables and Canvas draw blocks that read a token update when it changes.
    var palette by mutableStateOf(ZenLight)
        internal set

    val Bg get() = palette.bg
    val Surface get() = palette.surface
    val Surface2 get() = palette.surface2
    val Line get() = palette.line
    val Ink get() = palette.ink
    val Muted get() = palette.muted
    val Faint get() = palette.faint
    val Pine get() = palette.pine
    val Pine2 get() = palette.pine2
    val PineSoft get() = palette.pineSoft
    val Ice get() = palette.ice
    val Ice2 get() = palette.ice2
    val Ice3 get() = palette.ice3
    val IceHi get() = palette.iceHi
    val Red get() = palette.red
    val OnPine get() = palette.onPine
    val Track get() = palette.track
    val Shadow get() = palette.shadow
    val Halo get() = palette.halo
    val Orb get() = palette.orb

    val RLg = RoundedCornerShape(24.dp)
    val RMd = RoundedCornerShape(20.dp)
    val RSm = RoundedCornerShape(14.dp)
}

// Static weights: Android ignores variation settings on resource fonts in Compose's cache, so the
// variable font rendered every weight as Regular.
val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

object ZenType {
    val Title get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.025).em, lineHeight = 32.sp, color = Zen.Ink)
    val Sub get() = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 21.sp, color = Zen.Muted)
    val Eyebrow get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.12.em, color = Zen.Faint)
    val Strong get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, letterSpacing = (-0.01).em, color = Zen.Ink)
    val Body get() = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp, color = Zen.Ink)
    val Muted get() = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp, color = Zen.Muted)
    val Small get() = TextStyle(fontFamily = Inter, fontSize = 12.sp, color = Zen.Faint)
    val Section get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = (-0.01).em, color = Zen.Ink)
    val Phase get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 24.sp, letterSpacing = (-0.02).em, color = Zen.Pine)
    val BigNumber get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 60.sp, letterSpacing = (-0.04).em, color = Zen.Pine)
    val Unit get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.08.em, color = Zen.Pine2)
    val Button get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

@Composable
fun SwissZenTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Set before any child reads a token; a light/dark switch recreates the activity anyway.
    Zen.palette = if (dark) ZenDark else ZenLight
    // Bar icons follow the app's theme, not the system's: dark icons on ivory, light ones on the night ground.
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    val colors = if (dark) darkColorScheme(
        primary = Zen.Pine,
        onPrimary = Zen.OnPine,
        secondary = Zen.Red,
        background = Zen.Bg,
        surface = Zen.Surface,
        onBackground = Zen.Ink,
        onSurface = Zen.Ink,
        outline = Zen.Line,
    ) else lightColorScheme(
        primary = Zen.Pine,
        onPrimary = Zen.OnPine,
        secondary = Zen.Red,
        background = Zen.Bg,
        surface = Zen.Surface,
        onBackground = Zen.Ink,
        onSurface = Zen.Ink,
        outline = Zen.Line,
    )
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography.copy(
            bodyLarge = ZenType.Body,
            bodyMedium = ZenType.Body.copy(fontSize = 14.sp),
            labelLarge = ZenType.Button,
        ),
        content = content,
    )
}
