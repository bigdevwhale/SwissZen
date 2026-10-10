package app.swisszen.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.swisszen.AppContainer
import app.swisszen.R
import app.swisszen.data.SessionLog
import app.swisszen.ui.Routes
import app.swisszen.ui.components.ChipLabel
import app.swisszen.ui.components.IconTile
import app.swisszen.ui.components.ZIcon
import app.swisszen.ui.components.ZIconView
import app.swisszen.ui.components.ZIcons
import app.swisszen.ui.components.ZenCard
import app.swisszen.ui.components.tap
import app.swisszen.ui.components.zenShadow
import app.swisszen.ui.containerFactory
import app.swisszen.ui.practices.Catalog
import app.swisszen.ui.startMillis
import app.swisszen.ui.streak
import app.swisszen.ui.timeOf
import app.swisszen.ui.toLocalDateTime
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenLight
import app.swisszen.ui.theme.ZenType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime

data class HomeState(
    val breathedMin: Int = 0,
    val bells: Int = 0,
    val lines: Int = 0,
    val streak: Int = 0,
    val lastBreathe: Long? = null,
    val lastBell: Long? = null,
    val lastPractice: SessionLog? = null,
    val lastJournal: Long? = null,
)

class HomeViewModel(c: AppContainer) : ViewModel() {
    private val todayStart = LocalDate.now().startMillis()
    val state = combine(
        c.db.sessions().since(todayStart),
        c.db.bell().recent(200),
        c.db.journal().allTimes(),
        c.db.sessions().allTimes(),
        combine(
            c.db.sessions().lastOfAny(listOf(SessionLog.GUIDED, SessionLog.WIM_HOF)),
            c.db.sessions().lastOfAny(listOf(SessionLog.PRACTICE, SessionLog.MEDITATION)),
        ) { b, p -> b to p },
    ) { today, bells, journalTimes, sessionTimes, (lastBreathe, lastPractice) ->
        val breathSec = today.filter { it.kind == SessionLog.GUIDED || it.kind == SessionLog.WIM_HOF }.sumOf { it.durationSec }
        val days = (journalTimes + sessionTimes).map { it.toLocalDateTime().toLocalDate() }.toSet()
        HomeState(
            breathedMin = (breathSec + 30) / 60,
            bells = bells.count { it.rangAt >= todayStart },
            lines = journalTimes.count { it >= todayStart },
            streak = streak(days),
            lastBreathe = lastBreathe?.startedAt,
            lastBell = bells.firstOrNull()?.rangAt,
            lastPractice = lastPractice,
            lastJournal = journalTimes.firstOrNull(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
}

@Composable
private fun whenLabel(ms: Long?): String? {
    ms ?: return null
    val dt = ms.toLocalDateTime()
    val day = when (dt.toLocalDate()) {
        LocalDate.now() -> stringResource(R.string.today)
        LocalDate.now().minusDays(1) -> stringResource(R.string.yesterday)
        else -> return dt.toLocalDate().toString()
    }
    return "$day, ${timeOf(ms)}"
}

@Composable
fun HomeScreen(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val vm: HomeViewModel = viewModel(factory = containerFactory { HomeViewModel(it) })
    val s by vm.state.collectAsStateWithLifecycle()
    val hour = LocalTime.now().hour
    val greet = stringResource(if (hour < 12) R.string.greet_morning else if (hour < 18) R.string.greet_afternoon else R.string.greet_evening)
    val never = stringResource(R.string.never_used)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo(Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Text("SwissZen", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.02).em, color = Zen.Pine)
            Spacer(Modifier.weight(1f))
            if (s.streak > 0) ChipLabel(pluralStringResource(R.plurals.days_streak, s.streak, s.streak), ZIcons.Flame)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).tap { onSettings() }, contentAlignment = Alignment.Center) {
                ZIconView(ZIcons.Gear, Zen.Pine, size = 22.dp)
            }
        }

        Column(Modifier.padding(start = 2.dp, end = 2.dp, top = 18.dp, bottom = 22.dp)) {
            Text(greet.uppercase(), style = ZenType.Eyebrow)
            Text(stringResource(R.string.home_title), style = ZenType.Title, modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.home_sub), style = ZenType.Sub, modifier = Modifier.padding(top = 6.dp))
        }

        val practiceName = s.lastPractice?.let { log ->
            (Catalog.practices.firstOrNull { it.id == log.detail }?.name ?: Catalog.meditations.firstOrNull { it.id == log.detail }?.name)
        }?.let { stringResource(it) }
        val blades = listOf(
            Blade(Routes.BREATHE, ZIcons.Breathe, R.string.tab_breathe, R.string.tool_breathe_purpose,
                whenLabel(s.lastBreathe)?.let { stringResource(R.string.last_used, it) } ?: never),
            Blade(Routes.BELL, ZIcons.Bell, R.string.tab_bell, R.string.tool_bell_purpose,
                s.lastBell?.let { stringResource(R.string.last_rang, whenLabel(it)!!) } ?: never),
            Blade(Routes.PRACTICES, ZIcons.Lotus, R.string.tab_practices, R.string.tool_practices_purpose,
                whenLabel(s.lastPractice?.startedAt)?.let { stringResource(R.string.last_used, listOfNotNull(it, practiceName).joinToString(" · ")) } ?: never),
            Blade(Routes.JOURNAL, ZIcons.Pencil, R.string.tab_journal, R.string.tool_journal_purpose,
                whenLabel(s.lastJournal)?.let { stringResource(R.string.last_entry, it) } ?: never),
        )
        Knife(blades, onOpen)

        ZenCard(Modifier.fillMaxWidth().padding(top = 26.dp), padding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp, horizontal = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Stat(pluralStringResource(R.plurals.minutes_short, s.breathedMin, s.breathedMin), stringResource(R.string.stat_breathed), Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(Zen.Line))
                Stat("${s.bells}", stringResource(R.string.stat_bells), Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(Zen.Line))
                Stat("${s.lines}", stringResource(R.string.stat_lines), Modifier.weight(1f))
            }
        }
    }
}

private data class Blade(val route: String, val icon: ZIcon, val name: Int, val purpose: Int, val last: String)

private val easeOut = CubicBezierEasing(.22f, 1f, .36f, 1f)

@Composable
private fun Knife(blades: List<Blade>, onOpen: (String) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        // the handle / spine
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier.padding(start = 6.dp).width(16.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF2A6550), ZenLight.pine, Color(0xFF173D2F))))
            )
        }
        Column(Modifier.padding(start = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            blades.forEachIndexed { i, b ->
                val anim = remember { Animatable(0f) }
                LaunchedEffect(Unit) { delay(120L + i * 95L); anim.animateTo(1f, tween(1000, easing = easeOut)) }
                BladeCard(b, anim.value) { onOpen(b.route) }
            }
        }
    }
}

@Composable
private fun BladeCard(b: Blade, unfold: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 46.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
    Box(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = unfold
            rotationZ = -16f * (1 - unfold)
            translationX = -14.dp.toPx() * (1 - unfold)
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(-0.05f, 0.5f)
        }
    ) {
        Row(
            Modifier.fillMaxWidth().zenShadow(shape).clip(shape)
                .background(Brush.linearGradient(
                    0f to Zen.Surface, .62f to Zen.Surface, .88f to Zen.IceHi, 1f to Zen.Surface,
                ))
                .tap(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(b.icon)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(stringResource(b.name), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.015).em, color = Zen.Ink)
                Text(stringResource(b.purpose), fontFamily = Inter, fontSize = 13.5.sp, lineHeight = 18.sp, color = Zen.Muted, modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(Zen.Ice3, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(b.last, fontFamily = Inter, fontSize = 11.5.sp, color = Zen.Faint, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            ZIconView(ZIcons.Chevron, Zen.Faint, size = 20.dp, strokeWidth = 1.8f)
        }
        // rivet on the handle
        Canvas(Modifier.align(Alignment.CenterStart).padding(start = 0.dp).size(12.dp).graphicsLayer { translationX = -22.dp.toPx() }) {
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFCFD6D3), Color(0xFF9AA5A0)), center = Offset(size.width * .35f, size.height * .3f), radius = size.width))
            drawCircle(Color.Black.copy(alpha = .18f), style = Stroke(1.5f))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 22.sp, letterSpacing = (-0.02).em, color = Zen.Pine, maxLines = 1)
        Text(label, fontFamily = Inter, fontSize = 12.sp, color = Zen.Muted, maxLines = 1)
    }
}

/** Red rounded square with a Swiss cross whose top arm is a leaf. */
@Composable
fun Logo(modifier: Modifier) {
    val leaf = remember { PathParser().parsePathString("M15 15.2C11.6 13.2 11.7 8.6 15 5.6c3.3 3 3.4 7.6 0 9.6z").toPath() }
    val vein = remember { PathParser().parsePathString("M15 14.4V8.2").toPath() }
    Canvas(modifier) {
        val k = size.minDimension / 30f
        scale(k, k, pivot = Offset.Zero) {
            drawRoundRect(Zen.Red, size = Size(30f, 30f), cornerRadius = CornerRadius(9f))
            drawRoundRect(Color.White, Offset(7f, 12.6f), Size(16f, 5f), CornerRadius(1f))
            drawRoundRect(Color.White, Offset(12.5f, 15f), Size(5f, 8f), CornerRadius(1f))
            drawPath(leaf, Color.White)
            drawPath(vein, Zen.Red, style = Stroke(1f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}
