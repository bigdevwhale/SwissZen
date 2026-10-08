package app.swisszen.ui.practices

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.swisszen.AppContainer
import app.swisszen.R
import app.swisszen.data.SessionLog
import app.swisszen.ui.KeepScreenOn
import app.swisszen.ui.components.AppBar
import app.swisszen.ui.components.BtnStyle
import app.swisszen.ui.components.Dots
import app.swisszen.ui.components.IconTile
import app.swisszen.ui.components.Orb
import app.swisszen.ui.components.RingProgress
import app.swisszen.ui.components.RoundIconButton
import app.swisszen.ui.components.SectionHeader
import app.swisszen.ui.components.ZIcon
import app.swisszen.ui.components.ZIconView
import app.swisszen.ui.components.ZIcons
import app.swisszen.ui.components.ZenButton
import app.swisszen.ui.components.tap
import app.swisszen.ui.components.zenShadow
import app.swisszen.ui.containerFactory
import app.swisszen.ui.mmss
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PracticesViewModel(private val c: AppContainer) : ViewModel() {
    fun log(kind: String, id: String, startedAt: Long, seconds: Int) = viewModelScope.launch {
        c.db.sessions().insert(SessionLog(kind = kind, startedAt = startedAt, durationSec = seconds, detail = id))
    }
    fun chime(volume: Float) = c.sound.bell(volume)
}

@Composable
fun PracticesScreen(onPractice: (Int) -> Unit, onMeditation: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 28.dp)
    ) {
        AppBar(stringResource(R.string.practices_title))
        Text(stringResource(R.string.practices_sub), style = ZenType.Sub)
        SectionHeader(stringResource(R.string.micro_practices), stringResource(R.string.micro_range))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Catalog.practices.forEachIndexed { i, p ->
                PCard(p.icon, stringResource(p.name), stringResource(p.desc), pluralStringResource(R.plurals.minutes_short, p.minutes, p.minutes),
                    tileBg = if (i % 2 == 1) Zen.PineSoft else Zen.Ice) { onPractice(i) }
            }
        }
        SectionHeader(stringResource(R.string.meditations), stringResource(R.string.meditations_range))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Catalog.meditations.forEachIndexed { i, m ->
                PCard(m.icon, stringResource(m.name), stringResource(m.desc), pluralStringResource(R.plurals.minutes_short, m.minutes, m.minutes),
                    tileBg = Zen.Pine, tileTint = Color(0xFFEAF3F1)) { onMeditation(i) }
            }
        }
    }
}

@Composable
private fun PCard(icon: ZIcon, name: String, desc: String, dur: String, tileBg: Color, tileTint: Color = Zen.Pine, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().zenShadow(Zen.RMd).clip(Zen.RMd).background(Zen.Surface).tap(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, bg = tileBg, tint = tileTint)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(name, style = ZenType.Strong)
            Text(desc, style = ZenType.Muted, modifier = Modifier.padding(top = 2.dp))
        }
        Text(dur, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Zen.Pine,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Zen.PineSoft).padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun Glyph(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "glyph")
    val s by t.animateFloat(.96f, 1.04f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "glyphScale")
    Box(
        Modifier.size(132.dp).scale(s).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White, Color(0xFFE6F0F4), Color(0xFFC3DAE3)), center = Offset(150f, 120f), radius = 420f)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SessionTop(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(ZIcons.Back, stringResource(R.string.back_to_practices), onClick = onBack)
        Column(Modifier.padding(start = 10.dp)) {
            Text(title, style = ZenType.Strong)
            Text(subtitle, style = ZenType.Muted)
        }
    }
}

@Composable
fun PracticeSessionScreen(index: Int, onClose: () -> Unit) {
    val vm: PracticesViewModel = viewModel(factory = containerFactory { PracticesViewModel(it) })
    val p = Catalog.practices[index]
    val heads = stringArrayResource(p.heads)
    val hints = stringArrayResource(p.hints)
    var step by rememberSaveable { mutableIntStateOf(0) }
    val startedAt = remember { System.currentTimeMillis() }
    val done = step >= heads.size
    val dur = pluralStringResource(R.plurals.minutes_short, p.minutes, p.minutes)
    BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize().background(Zen.Bg).statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 22.dp)) {
        SessionTop(stringResource(p.name), stringResource(R.string.guided, dur), onClose)
        Dots(heads.size, step, Modifier.align(Alignment.CenterHorizontally).padding(top = 22.dp))
        AnimatedContent(step, Modifier.weight(1f).fillMaxWidth(), label = "step",
            transitionSpec = { (fadeIn(tween(400)) + slideInVertically { it / 30 }) togetherWith fadeOut(tween(200)) }) { s ->
            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Glyph {
                    val n = p.numbered?.getOrNull(s)
                    if (s < heads.size && n != null) Text(n, fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 52.sp, color = Zen.Pine)
                    else ZIconView(p.icon, Zen.Pine, size = 46.dp)
                }
                Spacer(Modifier.height(30.dp))
                if (s < heads.size) {
                    Text(stringResource(R.string.step_of, s + 1, heads.size), fontFamily = Inter, fontSize = 12.5.sp, letterSpacing = 0.04.em, color = Zen.Faint)
                    Spacer(Modifier.height(10.dp))
                    Text(heads[s], style = SessionHead, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 310.dp))
                    Text(hints[s], style = SessionHint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp).widthIn(max = 290.dp))
                } else {
                    Text(stringResource(R.string.practice_complete), style = SessionHead, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.practice_complete_hint), style = SessionHint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp).widthIn(max = 290.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZenButton(stringResource(if (done) R.string.repeat else if (step == 0) R.string.exit else R.string.back), Modifier.weight(1f), BtnStyle.Ghost) {
                when { done -> step = 0; step == 0 -> onClose(); else -> step-- }
            }
            val last = step == heads.size - 1
            ZenButton(
                stringResource(if (done) R.string.back_to_practices else if (last) R.string.finish else R.string.next),
                Modifier.weight(1.4f), if (last) BtnStyle.Red else BtnStyle.Pine,
            ) {
                if (done) onClose() else {
                    step++
                    if (step == heads.size) vm.log(SessionLog.PRACTICE, p.id, startedAt, ((System.currentTimeMillis() - startedAt) / 1000).toInt())
                }
            }
        }
    }
}

private val SessionHead = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 25.sp, lineHeight = 31.sp, letterSpacing = (-0.02).em, color = Zen.Ink)
private val SessionHint = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 22.sp, color = Zen.Muted)

@Composable
fun MeditationPlayerScreen(index: Int, onClose: () -> Unit) {
    val vm: PracticesViewModel = viewModel(factory = containerFactory { PracticesViewModel(it) })
    val m = Catalog.meditations[index]
    val titles = stringArrayResource(m.titles)
    val hints = stringArrayResource(m.hints)
    val total = m.seconds.sum().toDouble()
    var phase by remember { mutableIntStateOf(0) }
    var phaseT by remember { mutableDoubleStateOf(0.0) }
    var elapsed by remember { mutableDoubleStateOf(0.0) }
    var running by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var logged by remember { mutableStateOf(false) }
    val dur = pluralStringResource(R.plurals.minutes_short, m.minutes, m.minutes)

    fun finish() {
        if (done) return
        done = true; running = false
        vm.chime(0.7f)
        if (!logged && elapsed >= 30) { logged = true; vm.log(SessionLog.MEDITATION, m.id, startedAt, elapsed.toInt()) }
    }
    fun nextPhase() {
        if (phase < m.seconds.size - 1) { phase++; phaseT = 0.0; vm.chime(0.35f) } else finish()
    }

    LaunchedEffect(running, done) {
        while (running && !done) {
            delay(250)
            elapsed += .25; phaseT += .25
            if (phaseT >= m.seconds[phase]) nextPhase()
        }
    }
    BackHandler {
        if (!done && elapsed >= 30) finish()
        onClose()
    }
    if (running) KeepScreenOn()

    val breath = rememberInfiniteTransition(label = "med")
    val s by breath.animateFloat(.8f, .94f, infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse), label = "medScale")

    Column(Modifier.fillMaxSize().background(Zen.Bg).statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 22.dp)) {
        SessionTop(stringResource(m.name), stringResource(R.string.timed, dur)) {
            if (!done && elapsed >= 30) finish()
            onClose()
        }
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!done) {
                Box(Modifier.padding(top = 22.dp).size(236.dp), contentAlignment = Alignment.Center) {
                    RingProgress(Modifier.fillMaxSize(), (elapsed / total).toFloat(), Zen.Pine2, stroke = 4.dp)
                    Orb(Modifier.fillMaxSize().padding(18.dp), if (running) s else .87f)
                    Text(mmss(total - elapsed), fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 46.sp, letterSpacing = (-0.04).em, color = Zen.Pine)
                }
                Text(stringResource(R.string.part_of, phase + 1, titles.size), fontFamily = Inter, fontSize = 12.5.sp, letterSpacing = 0.04.em, color = Zen.Faint,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                AnimatedContent(phase, label = "medPhase", transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(250)) }) { ph ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(titles[ph], style = SessionHead, textAlign = TextAlign.Center)
                        Text(hints[ph], style = SessionHint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp).widthIn(max = 290.dp))
                    }
                }
                Dots(titles.size, phase, Modifier.padding(top = 22.dp))
            } else {
                Spacer(Modifier.weight(1f))
                Glyph { ZIconView(m.icon, Zen.Pine, size = 46.dp) }
                Spacer(Modifier.height(30.dp))
                Text(stringResource(R.string.sit_complete), style = SessionHead, textAlign = TextAlign.Center)
                Text(
                    if (elapsed < 60) stringResource(R.string.brief_sit) else stringResource(R.string.sat_for, mmss(elapsed)),
                    style = SessionHint, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp),
                )
                Spacer(Modifier.weight(1f))
            }
        }
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZenButton(stringResource(if (done) R.string.again else R.string.skip), Modifier.weight(1f), BtnStyle.Ghost) {
                if (done) {
                    phase = 0; phaseT = 0.0; elapsed = 0.0; done = false; logged = false
                    startedAt = System.currentTimeMillis(); running = true
                } else {
                    elapsed += m.seconds[phase] - phaseT
                    nextPhase()
                }
            }
            ZenButton(
                stringResource(if (done) R.string.back_to_practices else if (running) R.string.pause else R.string.resume),
                Modifier.weight(1.4f), BtnStyle.Pine,
            ) { if (done) onClose() else running = !running }
        }
    }
}
