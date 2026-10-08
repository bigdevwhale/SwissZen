package app.swisszen.ui.breathe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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
import app.swisszen.breath.GuidedPaceEngine
import app.swisszen.breath.GuidedPreset
import app.swisszen.breath.WimHofLevel
import app.swisszen.data.AppSettings
import app.swisszen.data.SessionLog
import app.swisszen.ui.KeepScreenOn
import app.swisszen.ui.LocalToast
import app.swisszen.ui.components.AppBar
import app.swisszen.ui.components.BtnStyle
import app.swisszen.ui.components.ChipLabel
import app.swisszen.ui.components.Orb
import app.swisszen.ui.components.PChip
import app.swisszen.ui.components.Segmented
import app.swisszen.ui.components.ZIconView
import app.swisszen.ui.components.ZIcons
import app.swisszen.ui.components.ZenButton
import app.swisszen.ui.components.ZenCard
import app.swisszen.ui.components.tap
import app.swisszen.ui.components.zenShadow
import app.swisszen.ui.containerFactory
import app.swisszen.ui.mmss2
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class BreatheViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun setLevel(id: String, expertRounds: Int) = viewModelScope.launch { c.settings.setWimHof(id, expertRounds) }
    fun log(kind: String, startedAt: Long, seconds: Int, detail: String?) = viewModelScope.launch {
        if (seconds >= 20) c.db.sessions().insert(SessionLog(kind = kind, startedAt = startedAt, durationSec = seconds, detail = detail))
    }
    /** [force] = the caller already decided (Wim Hof session has its own sound toggle). */
    fun breath(inhale: Boolean, seconds: Double, force: Boolean = false) { if (force || settings.value.sound) c.sound.breath(inhale, seconds) }
    fun bell() = c.sound.bell(0.5f)
}

@Composable
fun BreatheScreen(onStartWimHof: () -> Unit) {
    val vm: BreatheViewModel = viewModel(factory = containerFactory { BreatheViewModel(it) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    var method by rememberSaveable { mutableIntStateOf(0) }
    val guided = rememberGuidedState()
    DisposableEffect(Unit) { onDispose { guided.stop(vm) } } // leaving the tab ends (and logs) a running session

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 28.dp)
    ) {
        AppBar(stringResource(R.string.breathe_title)) {
            if (method == 0) ChipLabel(mmss2(guided.sessionLeft))
        }
        Segmented(
            listOf(stringResource(R.string.method_guided), stringResource(R.string.method_wimhof)), method,
            Modifier.padding(top = 4.dp, bottom = 18.dp),
        ) {
            method = it
            if (it != 0) guided.stop(vm)
        }
        if (method == 0) GuidedPanel(guided, vm, settings)
        else WimHofSetup(settings, onPick = vm::setLevel, onStart = onStartWimHof)
    }
}

// ---------------- Guided pace ----------------

class GuidedState {
    var preset by mutableStateOf(GuidedPreset.BOX)
    var minutes by mutableIntStateOf(5)
    var engine by mutableStateOf<GuidedPaceEngine?>(null)
    var running by mutableStateOf(false)
    var finished by mutableStateOf(false)
    var tick by mutableIntStateOf(0)       // bumps every engine second to recompose
    var startedAt by mutableLongStateOf(0L)
    val sessionLeft get() = engine?.sessionLeft ?: if (finished) 0 else minutes * 60

    fun stop(vm: BreatheViewModel) {
        engine?.let { e -> if (!finished) vm.log(SessionLog.GUIDED, startedAt, minutes * 60 - e.sessionLeft, preset.name.lowercase()) }
        engine = null; running = false; finished = false
    }
}

@Composable
fun rememberGuidedState() = remember { GuidedState() }

private val breathEase = CubicBezierEasing(.45f, .05f, .55f, .95f)
private const val G_MIN = 0.56f

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GuidedPanel(g: GuidedState, vm: BreatheViewModel, settings: AppSettings) {
    val haptic = LocalHapticFeedback.current
    val toast = LocalToast.current
    val doneMsg = stringResource(R.string.session_complete)
    val orb = remember { Animatable(0.6f) }
    val scope = rememberCoroutineScope()
    val idle = rememberInfiniteTransition(label = "idle")
    val idleScale by idle.animateFloat(.58f, .65f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "idleScale")

    // One-second clock while running.
    LaunchedEffect(g.running, g.engine) {
        val e = g.engine ?: return@LaunchedEffect
        while (g.running) {
            delay(1000)
            val newPhase = e.tickSecond()
            g.tick++
            if (e.done) {
                vm.log(SessionLog.GUIDED, g.startedAt, g.minutes * 60, g.preset.name.lowercase())
                toast(doneMsg)
                g.running = false; g.finished = true
                g.engine = null // cancels this effect; the orb effect settles the circle
            } else if (newPhase) {
                if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
    }
    // Orb follows the phase: grow on inhale, shrink on exhale, stay put on holds.
    // Re-runs on every phase change and on pause/resume (resuming animates over the seconds left).
    val e = g.engine
    LaunchedEffect(e, e?.phase, g.running) {
        if (e == null) { if (g.finished) orb.animateTo(0.6f, tween(2000)) else orb.stop(); return@LaunchedEffect }
        if (!g.running) { orb.stop(); return@LaunchedEffect }
        val target = if (e.phase == 0 || e.phase == 1) 1f else G_MIN
        if (e.phase == 0 || e.phase == 2) {
            vm.breath(e.phase == 0, e.left.toDouble())
            orb.animateTo(target, tween(e.left * 1000, easing = breathEase))
        } else orb.snapTo(target)
    }

    val pattern = g.preset.pattern
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuidedPreset.entries.forEach { p ->
            val name = stringResource(when (p) { GuidedPreset.BOX -> R.string.preset_box; GuidedPreset.CALM -> R.string.preset_calm; GuidedPreset.COHERENT -> R.string.preset_coherent })
            PChip(name, g.preset == p, p.pattern.filter { it > 0 }.joinToString("-")) {
                g.stop(vm); g.preset = p
                scope.launch { orb.snapTo(0.6f) }
            }
        }
    }

    // The breathing circle
    Box(Modifier.padding(top = 18.dp, bottom = 10.dp).size(286.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Zen.Ice2.copy(alpha = .9f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            drawCircle(Zen.Ice2.copy(alpha = .6f), radius = size.minDimension / 2 * .56f, style = Stroke(1.5.dp.toPx()))
        }
        val active = e != null
        Orb(Modifier.fillMaxSize(), if (active) orb.value else if (g.finished) orb.value else idleScale)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val big = when {
                e != null -> "${e.left}"
                g.finished -> "—"
                else -> g.preset.label
            }
            Text(big, style = ZenType.BigNumber.copy(fontSize = if (e != null) 60.sp else 40.sp))
            Text(
                stringResource(if (e != null) R.string.seconds else if (g.finished) R.string.well_done_short else R.string.pattern).uppercase(),
                style = ZenType.Unit, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
    val label = when {
        e == null -> stringResource(if (g.finished) R.string.session_complete else R.string.ready)
        !g.running -> stringResource(R.string.paused)
        else -> stringResource(listOf(R.string.phase_in, R.string.phase_hold, R.string.phase_out, R.string.phase_hold)[e.phase])
    }
    Text(label, style = ZenType.Phase, modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 22.dp), horizontalArrangement = Arrangement.Center) {
        val shorts = listOf(R.string.phase_in_short, R.string.phase_hold_short, R.string.phase_out_short, R.string.phase_hold_short)
        pattern.forEachIndexed { i, s ->
            if (s > 0) {
                val on = e != null && e.phase == i
                Text(
                    stringResource(R.string.phase_chip, stringResource(shorts[i]), s), fontFamily = Inter, fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    color = if (on) Zen.Pine else Zen.Faint,
                    modifier = Modifier.padding(horizontal = 3.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (on) Zen.PineSoft else Color.Transparent).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.session_length), style = ZenType.Muted, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.remaining) + " ", style = ZenType.Muted)
        Text(mmss2(g.sessionLeft), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Zen.Ink)
    }
    Spacer(Modifier.height(14.dp))
    Segmented(listOf(2, 5, 10).map { pluralStringResource(R.plurals.minutes_short, it, it) }, listOf(2, 5, 10).indexOf(g.minutes)) {
        g.stop(vm); g.minutes = listOf(2, 5, 10)[it]
        scope.launch { orb.snapTo(0.6f) }
    }
    Spacer(Modifier.height(14.dp))
    val btnLabel = when {
        e == null -> stringResource(if (g.finished) R.string.start_again else R.string.start)
        g.running -> stringResource(R.string.pause)
        else -> stringResource(R.string.resume)
    }
    ZenButton(btnLabel, Modifier.fillMaxWidth(), BtnStyle.Red, icon = if (g.running) ZIcons.Pause else ZIcons.Play) {
        when {
            e == null -> {
                g.finished = false
                g.engine = GuidedPaceEngine(g.preset, g.minutes)
                g.startedAt = System.currentTimeMillis()
                g.running = true
                scope.launch { orb.snapTo(G_MIN) }
            }
            else -> g.running = !g.running
        }
    }
    if (g.running) KeepScreenOn()
}

// ---------------- Wim Hof setup ----------------

@Composable
fun levelName(id: String) = stringResource(when (id) { "med" -> R.string.wh_medium; "adv" -> R.string.wh_advanced; "exp" -> R.string.wh_expert; else -> R.string.wh_beginner })

@Composable
fun levelPace(id: String) = stringResource(when (id) { "med" -> R.string.wh_pace_medium; "adv" -> R.string.wh_pace_advanced; "exp" -> R.string.wh_pace_expert; else -> R.string.wh_pace_beginner })

@Composable
private fun WimHofSetup(settings: AppSettings, onPick: (String, Int) -> Unit, onStart: () -> Unit) {
    val level = WimHofLevel.byId(settings.whLevel)
    val rounds = if (level.id == "exp") settings.whExpertRounds else level.defaultRounds

    Text(stringResource(R.string.wh_intro), style = ZenType.Muted, modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp))
    WimHofLevel.ALL.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { l ->
                LevelCard(l, l.id == level.id, settings.whExpertRounds, Modifier.weight(1f),
                    onPick = { onPick(l.id, settings.whExpertRounds) },
                    onRounds = { onPick("exp", it) })
            }
        }
    }
    Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)) {
        ZIconView(ZIcons.Shield, Zen.Faint, size = 16.dp, strokeWidth = 1.7f, modifier = Modifier.padding(top = 1.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.wh_safety), fontFamily = Inter, fontSize = 12.sp, lineHeight = 18.sp, color = Zen.Muted)
    }

    ZenCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        SpecRow(stringResource(R.string.wh_spec_rounds), "$rounds")
        SpecRow(stringResource(R.string.wh_spec_breaths), level.breathsLabel)
        SpecRow(stringResource(R.string.wh_spec_pace), levelPace(level.id))
        SpecRow(stringResource(R.string.wh_spec_tempo), stringResource(R.string.wh_spec_tempo_value, "%.1f".format(level.inhale), "%.1f".format(level.exhale)))
        SpecRow(stringResource(R.string.wh_spec_session), stringResource(R.string.wh_spec_session_value, (level.estimateSeconds(rounds) / 60).roundToInt()), divider = false)
        Text(stringResource(R.string.wh_ladder), fontFamily = Inter, fontSize = 12.sp, color = Zen.Muted, modifier = Modifier.padding(start = 2.dp, top = 14.dp))
        Ladder(level, rounds)
        ZenButton(stringResource(R.string.wh_start), Modifier.fillMaxWidth(), BtnStyle.Red, icon = ZIcons.Play, onClick = onStart)
    }
}

@Composable
private fun LevelCard(l: WimHofLevel, selected: Boolean, expertRounds: Int, modifier: Modifier, onPick: () -> Unit, onRounds: (Int) -> Unit) {
    val shape = Zen.RMd
    Box(
        modifier
            .heightIn(min = 108.dp)
            .zenShadow(shape)
            .clip(shape)
            .background(Zen.Surface)
            .border(1.5.dp, if (selected) Zen.Pine else Color.Transparent, shape)
            .semantics { this.selected = selected }
            .tap(onClick = onPick)
            .padding(14.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(levelName(l.id), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = (-0.01).em, color = Zen.Ink)
                if (l.id == "med") Text(" · " + stringResource(R.string.wh_medium_tag), fontFamily = Inter, fontSize = 13.sp, color = Zen.Muted)
            }
            if (l.id == "exp") {
                Text(stringResource(R.string.wh_breaths_range, l.breathsLabel), style = ZenType.Muted.copy(fontSize = 12.5.sp), modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepBtn(ZIcons.Minus, stringResource(R.string.wh_fewer_rounds)) { onRounds((expertRounds - 1).coerceIn(4, 5)) }
                    Text("$expertRounds", fontFamily = Inter, fontWeight = FontWeight.SemiBold, color = Zen.Pine, modifier = Modifier.padding(horizontal = 8.dp))
                    StepBtn(ZIcons.Plus, stringResource(R.string.wh_more_rounds)) { onRounds((expertRounds + 1).coerceIn(4, 5)) }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wh_rounds_word), style = ZenType.Small)
                }
            } else {
                Text(
                    stringResource(R.string.wh_level_meta, pluralStringResource(R.plurals.wh_rounds, l.defaultRounds, l.defaultRounds), stringResource(R.string.wh_breaths_range, l.breathsLabel)),
                    style = ZenType.Muted.copy(fontSize = 12.5.sp), modifier = Modifier.padding(top = 3.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(levelPace(l.id), fontFamily = Inter, fontSize = 11.5.sp, color = Zen.Faint)
            }
        }
        if (selected) Box(Modifier.align(Alignment.TopEnd).size(20.dp).background(Zen.Pine, CircleShape), contentAlignment = Alignment.Center) {
            ZIconView(ZIcons.Check, Color.White, size = 12.dp, strokeWidth = 3f)
        }
    }
}

@Composable
private fun StepBtn(icon: app.swisszen.ui.components.ZIcon, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(Zen.PineSoft).semantics { contentDescription = label }.tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ZIconView(icon, Zen.Pine, size = 14.dp, strokeWidth = 2.4f) }
}

@Composable
private fun SpecRow(label: String, value: String, divider: Boolean = true) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontFamily = Inter, fontSize = 13.5.sp, color = Zen.Muted, modifier = Modifier.weight(1f))
            Text(value, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = Zen.Ink)
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(Zen.Line))
    }
}

@Composable
private fun Ladder(level: WimHofLevel, rounds: Int) {
    val holds = level.holds.take(rounds)
    val max = holds.max().toFloat()
    Row(
        Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom,
    ) {
        holds.forEachIndexed { i, h ->
            val mix = .22f + (if (rounds > 1) i / (rounds - 1f) else 1f) * .78f
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.width(46.dp).height((14 + h / max * 46).dp)
                        .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(lerp(Zen.Ice2, Zen.Pine, mix))
                )
                Text(level.ladder[i], fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Zen.Muted, maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

private fun lerp(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)
