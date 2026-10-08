package app.swisszen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One SVG sub-path of an icon: stroked by default, optionally filled and/or faded. */
data class IconPart(val d: String, val fill: Boolean = false, val alpha: Float = 1f)

/** The line icons from the prototype, kept as SVG path data on their original viewport. */
class ZIcon(val viewport: Float, vararg val parts: IconPart)

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"
private fun p(d: String, fill: Boolean = false, alpha: Float = 1f) = IconPart(d, fill, alpha)

object ZIcons {
    val Knife = ZIcon(24f, p("M6,14h12a3,3 0 0 1 3,3a3,3 0 0 1 -3,3H6a3,3 0 0 1 -3,-3a3,3 0 0 1 3,-3z"),
        p("M6 14 17.6 5.2c.9-.7 2.1.4 1.4 1.4L13.5 14"), p(circle(6.5f, 17f, 0.9f), fill = true))
    val Breathe = ZIcon(24f, p(circle(12f, 12f, 3f)), p(circle(12f, 12f, 6.5f), alpha = .6f), p(circle(12f, 12f, 10f), alpha = .3f))
    val Bell = ZIcon(24f, p("M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"), p("M10 20.5a2 2 0 0 0 4 0"))
    val Lotus = ZIcon(24f, p("M12 20c-4 0-8-2.5-8-6 2.5 0 5.5 1 8 4 2.5-3 5.5-4 8-4 0 3.5-4 6-8 6z"),
        p("M12 18c-2.2-2.6-2.6-6.2 0-10.5 2.6 4.3 2.2 7.9 0 10.5z"))
    val Pencil = ZIcon(24f, p("M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16z"), p("m13.5 6.5 4 4"))
    val Chevron = ZIcon(24f, p("M9 6l6 6-6 6"))
    val Back = ZIcon(24f, p("M15 5l-7 7 7 7"))
    val Flame = ZIcon(16f, p("M8 14.5c2.9 0 4.8-2 4.8-4.6 0-2.9-2.4-4.5-3.3-7.4C7.2 4.2 7 5.8 7.4 7.3 6.1 6.8 5.4 5.6 5.3 4.6 3.8 6 3.2 7.8 3.2 9.9c0 2.6 1.9 4.6 4.8 4.6z"))
    val Play = ZIcon(20f, p("M6 4.5v11l9-5.5z", fill = true))
    val Pause = ZIcon(20f, p("M5.5 4h3v12h-3zM11.5 4h3v12h-3z", fill = true))
    val Shuffle = ZIcon(24f, p("M4 7h11l-3-3M20 17H9l3 3"))
    val Shield = ZIcon(24f, p("M12 3l7 3v5c0 4.4-3 7.4-7 9-4-1.6-7-4.6-7-9V6l7-3z"), p("M12 9v4.4"), p(circle(12f, 16.6f, 0.7f), fill = true))
    val Check = ZIcon(24f, p("M4.5 12.5l5 5L19.5 7"))
    val Speaker = ZIcon(24f, p("M4 9.5h3.5L12 5.5v13l-4.5-4H4z"), p("M15.5 9a4 4 0 0 1 0 6M18 6.5a7.5 7.5 0 0 1 0 11"))
    val SpeakerOff = ZIcon(24f, p("M4 9.5h3.5L12 5.5v13l-4.5-4H4z"), p("M16 9.5l5 5M21 9.5l-5 5"))
    // Gear outline from Lucide (ISC license).
    val Gear = ZIcon(24f, p(circle(12f, 12f, 3f)),
        p("M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"))
    val Close = ZIcon(14f, p("m4 4 6 6M10 4l-6 6"))
    val Plus = ZIcon(24f, p("M12 6v12M6 12h12"))
    val Minus = ZIcon(24f, p("M6 12h12"))
    val Note = ZIcon(24f, p("M5 4h14v12l-4 4H5z"), p("M15 20v-4h4"), p("M8.5 9h7M8.5 12.5h4.5"))

    // Practice / meditation icons
    val Ground = ZIcon(24f, p(circle(12f, 12f, 8.5f)), p(circle(12f, 12f, 4.5f)), p(circle(12f, 12f, 1f), fill = true))
    val Body = ZIcon(24f, p(circle(12f, 5f, 2.2f)), p("M12 8v7M8 10.5l4-1.5 4 1.5M9.5 21l2.5-6 2.5 6"))
    val Eye = ZIcon(24f, p("M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"), p(circle(12f, 12f, 3f)))
    val Sunrise = ZIcon(24f, p("M3 18h18M6.5 18a5.5 5.5 0 0 1 11 0M12 6.5V4M5.6 9.6 4 8M18.4 9.6 20 8"))
    val Heart = ZIcon(24f, p("M12 19.5s-7.5-4.4-7.5-10A4.2 4.2 0 0 1 12 7a4.2 4.2 0 0 1 7.5 2.5c0 5.6-7.5 10-7.5 10z"))
    val Wave = ZIcon(24f, p("M3 10c2.5-3 5-3 7.5 0s5 3 7.5 0"), p("M3 15.5c2.5-3 5-3 7.5 0s5 3 7.5 0", alpha = .5f))
    val Mountain = ZIcon(24f, p("M3 19h18l-6.5-11-3.4 5.8L9 10.5 3 19z"))
    val Steps = ZIcon(24f, p("M8 4.5c1.8 0 3 1.4 3 3.4 0 2.6-1.6 4.6-3 6.1-1.4-1.5-3-3.5-3-6.1 0-2 1.2-3.4 3-3.4z"),
        p("M16 9.5c1.8 0 3 1.4 3 3.4 0 2.6-1.6 4.6-3 6.1-1.4-1.5-3-3.5-3-6.1 0-2 1.2-3.4 3-3.4z", alpha = .55f))
    val Moon = ZIcon(24f, p("M19.5 14.5A8 8 0 0 1 9.5 4.5a8 8 0 1 0 10 10z"))
}

@Composable
fun ZIconView(icon: ZIcon, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, strokeWidth: Float = 1.6f) {
    val paths = remember(icon) { icon.parts.map { it to PathParser().parsePathString(it.d).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / icon.viewport
        scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            paths.forEach { (part, path) ->
                if (part.fill) drawPath(path, tint, alpha = part.alpha, style = Fill)
                else drawPath(path, tint, alpha = part.alpha,
                    style = Stroke(width = strokeWidth * (icon.viewport / 24f), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
