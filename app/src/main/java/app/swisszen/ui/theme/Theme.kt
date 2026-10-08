package app.swisszen.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.swisszen.R

/** Design tokens taken one-to-one from the approved prototype. */
object Zen {
    val Bg = Color(0xFFF7F4EE)
    val Surface = Color(0xFFFFFFFF)
    val Surface2 = Color(0xFFFBF9F5)
    val Line = Color(0xFFE8E2D7)
    val Ink = Color(0xFF1B2621)
    val Muted = Color(0xFF6C7671)
    val Faint = Color(0xFFA0A6A2)
    val Pine = Color(0xFF1E4D3B)
    val Pine2 = Color(0xFF2E6A52)
    val PineSoft = Color(0xFFE4ECE7)
    val Ice = Color(0xFFDFECF2)
    val Ice2 = Color(0xFFB9D4DF)
    val Ice3 = Color(0xFF7FA8BA)
    val Red = Color(0xFFE30613)
    val OnPine = Color(0xFFF7F4EE)

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
    val Title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.025).em, lineHeight = 32.sp, color = Zen.Ink)
    val Sub = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 21.sp, color = Zen.Muted)
    val Eyebrow = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.12.em, color = Zen.Faint)
    val Strong = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, letterSpacing = (-0.01).em, color = Zen.Ink)
    val Body = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp, color = Zen.Ink)
    val Muted = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp, color = Zen.Muted)
    val Small = TextStyle(fontFamily = Inter, fontSize = 12.sp, color = Zen.Faint)
    val Section = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = (-0.01).em, color = Zen.Ink)
    val Phase = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 24.sp, letterSpacing = (-0.02).em, color = Zen.Pine)
    val BigNumber = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 60.sp, letterSpacing = (-0.04).em, color = Zen.Pine)
    val Unit = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.08.em, color = Zen.Pine2)
    val Button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

@Composable
fun SwissZenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Zen.Pine,
            onPrimary = Zen.OnPine,
            secondary = Zen.Red,
            background = Zen.Bg,
            surface = Zen.Surface,
            onBackground = Zen.Ink,
            onSurface = Zen.Ink,
            outline = Zen.Line,
        ),
        typography = MaterialTheme.typography.copy(
            bodyLarge = ZenType.Body,
            bodyMedium = ZenType.Body.copy(fontSize = 14.sp),
            labelLarge = ZenType.Button,
        ),
        content = content,
    )
}
