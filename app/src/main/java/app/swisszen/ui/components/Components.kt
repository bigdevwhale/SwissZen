package app.swisszen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenType
import kotlin.math.hypot

/** Click without the Material ripple — the prototype uses scale/colour feedback instead. */
fun Modifier.tap(role: Role = Role.Button, enabled: Boolean = true, onClick: () -> Unit) = this.then(
    Modifier.clickable(
        interactionSource = null,
        indication = null,
        role = role,
        enabled = enabled,
        onClick = onClick,
    )
)

/** Soft pine-tinted shadow like the prototype's --sh-1. */
fun Modifier.zenShadow(shape: Shape, elevation: Dp = 8.dp) =
    shadow(elevation, shape, clip = false, ambientColor = Zen.Shadow.copy(alpha = .25f), spotColor = Zen.Shadow.copy(alpha = .25f))

@Composable
fun ZenCard(modifier: Modifier = Modifier, shape: Shape = Zen.RLg, padding: PaddingValues = PaddingValues(18.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .zenShadow(shape)
            .clip(shape)
            .background(Zen.Surface)
            .padding(padding),
        content = content,
    )
}

/** The breathing sphere: radial gradient lit from the upper left, with a soft drop shadow. */
@Composable
fun Orb(modifier: Modifier, scale: Float, dim: Float = 0f) {
    Canvas(modifier) {
        val d = size.minDimension * scale
        val r = d / 2
        val c = center
        // drop shadow
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Zen.Shadow.copy(alpha = .22f), Color.Transparent),
                center = c + Offset(0f, r * .32f), radius = r * 1.05f,
            ),
            radius = r * 1.05f, center = c + Offset(0f, r * .32f),
        )
        val origin = Offset(c.x - r + d * .38f, c.y - r + d * .30f)
        val reach = hypot(d * .62f, d * .70f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Zen.Orb[0], .22f to Zen.Orb[1], .55f to Zen.Orb[2],
                .82f to Zen.Orb[3], 1f to Zen.Orb[4],
                center = origin, radius = reach,
            ),
            radius = r, center = c,
        )
        // inner bottom shade
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.Transparent, Zen.Shadow.copy(alpha = .10f)), center = c - Offset(0f, r * .25f), radius = r * 1.25f),
            radius = r, center = c,
        )
        if (dim > 0f) drawCircle(Zen.Bg.copy(alpha = .35f * dim), radius = r, center = c)
    }
}

@Composable
fun RingProgress(modifier: Modifier, progress: Float, color: Color, track: Color = Zen.Line, stroke: Dp = 5.dp, trackStroke: Dp = 3.dp) {
    Canvas(modifier) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = Size(size.width - w, size.height - w)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(trackStroke.toPx()))
        if (progress > 0f) drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize,
            style = Stroke(w, cap = StrokeCap.Round))
    }
}

enum class BtnStyle { Red, Pine, Ghost, Line }

@Composable
fun ZenButton(text: String, modifier: Modifier = Modifier, style: BtnStyle = BtnStyle.Pine, icon: ZIcon? = null, height: Dp = 52.dp, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val (bg, fg) = when (style) {
        BtnStyle.Red -> Zen.Red to Color.White
        BtnStyle.Pine -> Zen.Pine to Zen.OnPine
        BtnStyle.Ghost -> Zen.PineSoft to Zen.Pine
        BtnStyle.Line -> Color.Transparent to Zen.Pine
    }
    Row(
        modifier
            .height(height)
            .then(if (style == BtnStyle.Red) Modifier.shadow(10.dp, shape, ambientColor = Zen.Red, spotColor = Zen.Red) else Modifier)
            .clip(shape)
            .background(bg)
            .then(if (style == BtnStyle.Line) Modifier.border(1.5.dp, Zen.Pine, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { ZIconView(icon, fg, size = 20.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, style = ZenType.Button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Pill chip used for presets and intervals; [small] is the faint suffix ("4-4-4-4"). */
@Composable
fun PChip(text: String, selected: Boolean, small: String? = null, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Zen.Pine else Zen.Surface, label = "chip")
    val fg = if (selected) Zen.OnPine else Zen.Muted
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (selected) Zen.Pine else Zen.Line, RoundedCornerShape(20.dp))
            .semantics { this.selected = selected }
            .tap(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, color = fg)
        if (small != null) {
            Spacer(Modifier.width(6.dp))
            Text(small, fontFamily = Inter, fontSize = 12.sp, color = if (selected) Zen.Ice2 else Zen.Faint)
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Zen.PineSoft)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(11.dp)) else Modifier)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Zen.Surface else Color.Transparent)
                    .semantics { this.selected = on }
                    .tap(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.5.sp,
                    color = if (on) Zen.Pine else Zen.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ZenSwitch(checked: Boolean, red: Boolean = false, label: String, onChange: (Boolean) -> Unit) {
    val x by animateFloatAsState(if (checked) 20f else 0f, label = "switch")
    val bg by animateColorAsState(if (!checked) Zen.Track else if (red) Zen.Red else Zen.Pine, label = "switchBg")
    Box(
        Modifier
            .size(52.dp, 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "on" else "off"
            }
            .tap(role = Role.Switch) { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset((4 + x).dp.roundToPx(), 4.dp.roundToPx()) }
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun IconTile(icon: ZIcon, modifier: Modifier = Modifier, bg: Color = Zen.Ice, tint: Color = Zen.Pine, size: Dp = 48.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(15.dp)).background(bg), contentAlignment = Alignment.Center) {
        ZIconView(icon, tint, size = 24.dp)
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = ZenType.Section, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = ZenType.Small.copy(fontSize = 12.5.sp))
    }
}

@Composable
fun AppBar(title: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(52.dp).padding(bottom = 0.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = ZenType.Title, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun ChipLabel(text: String, icon: ZIcon? = null, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(32.dp)
            .zenShadow(RoundedCornerShape(16.dp), 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Zen.Surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { ZIconView(icon, Zen.Pine, size = 14.dp); Spacer(Modifier.width(6.dp)) }
        Text(text, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, color = Zen.Pine, maxLines = 1)
    }
}

@Composable
fun RoundIconButton(icon: ZIcon, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(44.dp)
            .zenShadow(CircleShape, 4.dp)
            .clip(CircleShape)
            .background(Zen.Surface)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ZIconView(icon, Zen.Pine, size = 22.dp, strokeWidth = 1.8f) }
}

/** Progress dots: done (ice), current (wide pine), upcoming (line). */
@Composable
fun Dots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val w by animateFloatAsState(if (i == current) 24f else 8f, label = "dot")
            Box(
                Modifier.size(w.dp, 8.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (i == current) Zen.Pine else if (i < current) Zen.Ice3 else Zen.Line)
            )
        }
    }
}

@Composable
fun CenterText(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) =
    Text(text, style = style, textAlign = TextAlign.Center, modifier = modifier.fillMaxWidth())
