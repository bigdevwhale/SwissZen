package app.swisszen.ui.breathe

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.swisszen.R
import app.swisszen.breath.WhPhase
import app.swisszen.container
import app.swisszen.data.AppSettings
import kotlinx.coroutines.flow.first
import app.swisszen.breath.WimHofEngine
import app.swisszen.breath.WimHofLevel
import app.swisszen.data.SessionLog
import app.swisszen.ui.KeepScreenOn
import app.swisszen.ui.components.BtnStyle
import app.swisszen.ui.components.Dots
import app.swisszen.ui.components.Orb
import app.swisszen.ui.components.RingProgress
import app.swisszen.ui.components.ZIconView
import app.swisszen.ui.components.ZIcons
import app.swisszen.ui.components.ZenButton
import app.swisszen.ui.components.ZenCard
import app.swisszen.ui.components.tap
import app.swisszen.ui.containerFactory
import app.swisszen.ui.mmss
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenType
import kotlin.math.ceil

@Composable
fun WimHofSessionScreen(onClose: () -> Unit) {
    // The chosen level lives in DataStore; wait for the real value before building the engine.
    val store = LocalContext.current.container.settings
    val initial by produceState<AppSettings?>(null) { value = store.settings.first() }
    val s = initial
    if (s == null) {
        Box(Modifier.fillMaxSize().background(Zen.Bg))
        return
    }
    WimHofSession(s, onClose)
}

@Composable
private fun WimHofSession(initial: AppSettings, onClose: () -> Unit) {
    val vm: BreatheViewModel = viewModel(factory = containerFactory { BreatheViewModel(it) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val engine = remember {
        val level = WimHofLevel.resolve(initial.whLevel, initial.whCustomRounds, initial.whCustomBreaths)
        WimHofEngine(level, if (level.id == "exp") initial.whExpertRounds else level.defaultRounds, initial.whInhale, initial.whExhale)
    }
    val startedAt = remember { System.currentTimeMillis() }
    var frame by remember { mutableIntStateOf(0) }       // recomposition clock
    var sound by remember { mutableStateOf(initial.sound) }
    var logged by remember { mutableStateOf(false) }

    fun onEntered(p: WhPhase) {
        if (sound) when (p) {
            WhPhase.IN -> vm.breath(true, engine.inhale, force = true)
            WhPhase.OUT -> vm.breath(false, engine.exhale, force = true)
            WhPhase.LAST_IN, WhPhase.REC_IN -> vm.breath(true, WimHofEngine.LAST_IN, force = true)
            WhPhase.LAST_OUT -> vm.breath(false, WimHofEngine.LAST_OUT, force = true)
            WhPhase.REC_OUT -> vm.breath(false, WimHofEngine.REC_OUT, force = true)
            WhPhase.HOLD, WhPhase.DONE -> vm.bell()
            else -> Unit
        }
        if (settings.haptics && p != WhPhase.IN && p != WhPhase.OUT) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (engine.phase != WhPhase.DONE) {
            val now = withFrameNanos { it }
            engine.tick((now - last) / 1e9).forEach(::onEntered)
            last = now
            frame++
        }
    }
    LaunchedEffect(engine.phase == WhPhase.DONE) {
        if (engine.phase == WhPhase.DONE && !logged) {
            logged = true
            vm.log(SessionLog.WIM_HOF, startedAt, engine.total.toInt(), engine.level.id)
        }
    }
    BackHandler {
        if (engine.phase == WhPhase.DONE || (engine.round == 1 && engine.holds.isEmpty())) onClose() else engine.finish()
    }
    KeepScreenOn()

    check(frame >= 0) // reading the frame clock makes every frame recompose the engine readouts

    Column(
        Modifier.fillMaxSize().background(Zen.Bg).statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp)
    ) {
        if (engine.phase == WhPhase.DONE) {
            Summary(engine, onDone = onClose)
            return@Column
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.wh_round_of, engine.round, engine.rounds),
                fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Zen.Pine,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Zen.PineSoft).padding(horizontal = 13.dp, vertical = 7.dp),
            )
            Spacer(Modifier.weight(1f))
            val sndLabel = stringResource(R.string.wh_sound)
            Box(Modifier.size(40.dp).clip(CircleShape).semantics { contentDescription = sndLabel }.tap { sound = !sound }, contentAlignment = Alignment.Center) {
                ZIconView(if (sound) ZIcons.Speaker else ZIcons.SpeakerOff, if (sound) Zen.Pine else Zen.Faint, size = 20.dp, strokeWidth = 1.7f)
            }
            Text(
                stringResource(R.string.end), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Zen.Muted,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).tap {
                    if (engine.round == 1 && engine.holds.isEmpty() && engine.total < 6) onClose() else engine.finish()
                }.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }

        // Orb stage
        val hold = engine.phase == WhPhase.HOLD
        val scale = engine.orbScale()
        Box(
            Modifier.padding(top = 14.dp).size(310.dp).align(Alignment.CenterHorizontally)
                .then(if (hold) Modifier.tap { engine.breatheIn() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            RingProgress(Modifier.fillMaxSize(), engine.ringProgress(), if (hold) Zen.Red else Zen.Pine2)
            Canvas(Modifier.fillMaxSize().padding(16.dp)) {
                val glow = if (hold) .15f else .35f + .65f * (scale - WimHofEngine.S_EMPTY.toFloat()) / (WimHofEngine.S_MAX - WimHofEngine.S_EMPTY).toFloat()
                val r = size.minDimension / 2 * scale * 1.13f
                drawCircle(Brush.radialGradient(listOf(Color(0xFF7FA8BA).copy(alpha = .45f * glow), Color.Transparent), center, r), r, center)
            }
            Orb(Modifier.fillMaxSize().padding(22.dp), scale, dim = if (hold) 1f else 0f)
            CenterReadout(engine)
        }

        val (label, hint) = phaseText(engine)
        Text(
            if (engine.paused) stringResource(R.string.paused) else label,
            fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em, color = Zen.Pine,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp).heightIn(min = 36.dp),
        )
        Text(
            if (engine.paused) stringResource(R.string.wh_paused_hint) else hint,
            fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp, color = Zen.Muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 14.dp, end = 14.dp).heightIn(min = 42.dp),
        )
        Spacer(Modifier.weight(1f))
        Dots(engine.rounds, engine.round - 1, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        when {
            hold -> ZenButton(stringResource(R.string.wh_breathe_in), Modifier.fillMaxWidth(), BtnStyle.Red) { engine.breatheIn() }
            engine.isPausable || engine.paused ->
                ZenButton(stringResource(if (engine.paused) R.string.resume else R.string.pause), Modifier.fillMaxWidth(), BtnStyle.Ghost) { engine.togglePause() }
            else -> Spacer(Modifier.height(52.dp))
        }
    }
}

/** Reads the (non-state) engine, so it must re-run with every frame of the parent. */
@NonSkippableComposable
@Composable
private fun CenterReadout(e: WimHofEngine) {
    val (big, unit, isTime) = when (e.phase) {
        WhPhase.INTRO -> Triple("${ceil(WimHofEngine.INTRO - e.t).toInt().coerceAtLeast(1)}",
            if (e.round == 1) stringResource(R.string.wh_get_ready) else stringResource(R.string.wh_round_lower, e.round), false)
        WhPhase.IN, WhPhase.OUT -> Triple("${e.breath}", stringResource(R.string.wh_of, e.breathsThisRound), false)
        WhPhase.LAST_IN, WhPhase.LAST_OUT -> Triple("${e.breathsThisRound}", stringResource(R.string.wh_last_breath), false)
        WhPhase.HOLD -> {
            val label = e.level.ladder[e.round - 1]
            Triple(mmss(e.t), if (e.t > e.holdTarget) stringResource(R.string.wh_past_target, label) else stringResource(R.string.wh_target, label), true)
        }
        WhPhase.REC_HOLD -> Triple("${ceil(WimHofEngine.REC_HOLD - e.t).toInt().coerceAtLeast(1)}", stringResource(R.string.wh_hold_unit), false)
        else -> Triple("", "", false)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, style = ZenType.BigNumber.copy(fontSize = if (isTime) 56.sp else 68.sp), modifier = Modifier.heightIn(min = 68.dp))
        Text(unit.uppercase(), style = ZenType.Unit, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun phaseText(e: WimHofEngine): Pair<String, String> = when (e.phase) {
    WhPhase.INTRO -> if (e.round == 1) stringResource(R.string.wh_get_comfortable) to stringResource(R.string.wh_get_comfortable_hint)
        else stringResource(R.string.wh_round_n, e.round) to stringResource(R.string.wh_round_n_hint)
    WhPhase.IN -> stringResource(R.string.wh_fully_in) to stringResource(R.string.wh_fully_in_hint)
    WhPhase.OUT -> stringResource(R.string.wh_let_go) to stringResource(R.string.wh_let_go_hint)
    WhPhase.LAST_IN -> stringResource(R.string.wh_last_in) to ""
    WhPhase.LAST_OUT -> stringResource(R.string.wh_last_out) to stringResource(R.string.wh_last_out_hint)
    WhPhase.HOLD -> stringResource(R.string.wh_hold) to stringResource(if (e.t > e.holdTarget) R.string.wh_hold_past_hint else R.string.wh_hold_hint)
    WhPhase.REC_IN -> stringResource(R.string.wh_recovery) to stringResource(R.string.wh_recovery_hint)
    WhPhase.REC_HOLD -> stringResource(R.string.wh_hold_it) to stringResource(R.string.wh_hold_it_hint)
    WhPhase.REC_OUT -> stringResource(R.string.wh_let_go) to stringResource(if (e.round < e.rounds) R.string.wh_next_round else R.string.wh_last_round)
    WhPhase.DONE -> "" to ""
}

@Composable
private fun Summary(e: WimHofEngine, onDone: () -> Unit) {
    val longest = e.holds.maxOrNull() ?: 0.0
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier.padding(top = 30.dp).size(96.dp).align(Alignment.CenterHorizontally).clip(CircleShape)
                .background(Brush.radialGradient(Zen.Halo, center = Offset(100f, 80f))),
            contentAlignment = Alignment.Center,
        ) { ZIconView(ZIcons.Check, Zen.Pine, size = 40.dp, strokeWidth = 1.8f) }
        Text(stringResource(R.string.wh_well_done), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 28.sp, color = Zen.Pine,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp))
        Text(stringResource(R.string.wh_well_done_hint), style = ZenType.Muted.copy(fontSize = 14.sp), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp))

        ZenCard(Modifier.fillMaxWidth().padding(top = 22.dp), padding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp, horizontal = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                SumStat("${e.holds.size}/${e.rounds}", stringResource(R.string.wh_sum_rounds), Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(Zen.Line))
                SumStat(mmss(longest), stringResource(R.string.wh_sum_longest), Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(Zen.Line))
                SumStat(mmss(e.total), stringResource(R.string.wh_sum_total), Modifier.weight(1f))
            }
        }
        ZenCard(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            val targets = e.level.holds.take(e.rounds)
            val scale = (targets + longest.toInt()).max() * 1.08f
            targets.forEachIndexed { i, target ->
                val v = e.holds.getOrNull(i)
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.wh_sum_round, i + 1), fontFamily = Inter, fontSize = 12.5.sp, color = Zen.Muted, modifier = Modifier.width(72.dp))
                    Box(Modifier.weight(1f).height(16.dp), contentAlignment = Alignment.CenterStart) {
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Zen.Line))
                        if (v != null) Box(Modifier.fillMaxWidth((v / scale).toFloat().coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Zen.Pine2))
                        Box(Modifier.fillMaxWidth(target / scale).height(16.dp), contentAlignment = Alignment.CenterEnd) {
                            Box(Modifier.width(2.dp).fillMaxHeight().offset(x = 1.dp).background(Zen.Red))
                        }
                    }
                    Text(v?.let(::mmss) ?: "—", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = Zen.Ink,
                        textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
                }
            }
        }
        Text(stringResource(R.string.wh_sum_note), fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp, color = Zen.Faint, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
        Spacer(Modifier.weight(1f))
        ZenButton(stringResource(R.string.done), Modifier.fillMaxWidth(), BtnStyle.Pine, onClick = onDone)
    }
}

@Composable
private fun SumStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(value, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 20.sp, color = Zen.Pine)
        Text(label, fontFamily = Inter, fontSize = 11.5.sp, color = Zen.Muted, maxLines = 1)
    }
}
