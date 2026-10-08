package app.swisszen.ui.bell

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.swisszen.AppContainer
import app.swisszen.R
import app.swisszen.bell.BellAlarms
import app.swisszen.bell.BellConfig
import app.swisszen.bell.BellMode
import app.swisszen.data.BellEvent
import app.swisszen.ui.LocalToast
import app.swisszen.ui.components.AppBar
import app.swisszen.ui.components.BtnStyle
import app.swisszen.ui.components.PChip
import app.swisszen.ui.components.SectionHeader
import app.swisszen.ui.components.Segmented
import app.swisszen.ui.components.ZIconView
import app.swisszen.ui.components.ZIcons
import app.swisszen.ui.components.ZenButton
import app.swisszen.ui.components.ZenCard
import app.swisszen.ui.components.ZenSwitch
import app.swisszen.ui.components.tap
import app.swisszen.ui.containerFactory
import app.swisszen.ui.hhmm
import app.swisszen.ui.timeOf
import app.swisszen.ui.toLocalDateTime
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class BellViewModel(private val c: AppContainer) : ViewModel() {
    val config = c.settings.bell.stateIn(viewModelScope, SharingStarted.Eagerly, BellConfig())
    val nextAt = c.settings.nextBellAt.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val history = c.db.bell().recent(20).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun update(transform: (BellConfig) -> BellConfig) = viewModelScope.launch {
        c.settings.updateBell(transform)
        BellAlarms.reschedule(c.app)
    }
    fun preview() = c.sound.bell()
    fun note(id: Long, text: String) = viewModelScope.launch { c.db.bell().setNote(id, text) }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BellScreen() {
    val vm: BellViewModel = viewModel(factory = containerFactory { BellViewModel(it) })
    val c by vm.config.collectAsStateWithLifecycle()
    val nextAt by vm.nextAt.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toast = LocalToast.current
    val previewMsg = stringResource(R.string.bell_preview_toast)
    val scope = rememberCoroutineScope()
    val swing = remember { Animatable(0f) }
    var canNotify by remember { mutableStateOf(BellAlarms.canNotify(context)) }
    var noteFor by remember { mutableStateOf<BellEvent?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        if (granted) vm.update { it.copy(enabled = true) }
    }
    fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, DateFormat.is24HourFormat(context)).show()
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 28.dp)
    ) {
        AppBar(stringResource(R.string.bell_title))

        ZenCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Zen.Ice),
                    contentAlignment = Alignment.Center,
                ) {
                    ZIconView(ZIcons.Bell, Zen.Pine, size = 26.dp,
                        modifier = Modifier.graphicsLayer { rotationZ = swing.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, .1f) })
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(stringResource(R.string.bell_name), style = ZenType.Strong)
                    Text(nextLabel(c, nextAt), style = ZenType.Muted)
                }
                ZenSwitch(c.enabled, red = true, label = stringResource(R.string.bell_name)) { on ->
                    if (on && !canNotify && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.update { it.copy(enabled = on) }
                }
            }
            if (c.enabled && !canNotify) {
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.bell_need_permission), style = ZenType.Muted, modifier = Modifier.weight(1f))
                    if (Build.VERSION.SDK_INT >= 33) TextButton({ permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(stringResource(R.string.bell_allow), color = Zen.Red, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Column(Modifier.alpha(if (c.enabled) 1f else .45f)) {
            SectionHeader(stringResource(R.string.bell_when))
            ZenCard(Modifier.fillMaxWidth()) {
                Segmented(
                    listOf(stringResource(R.string.bell_mode_random), stringResource(R.string.bell_mode_interval), stringResource(R.string.bell_mode_times)),
                    c.mode.ordinal,
                ) { i -> vm.update { it.copy(mode = BellMode.entries[i]) } }
                Column(
                    Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(16.dp)).background(Zen.Surface2)
                        .border(1.dp, Zen.Line, RoundedCornerShape(16.dp)).padding(14.dp)
                ) {
                    when (c.mode) {
                        BellMode.RANDOM -> {
                            Text(stringResource(R.string.bell_random_desc, c.randomMin, c.randomMax), style = ZenType.Muted)
                            var range by remember(c.randomMin, c.randomMax) { mutableStateOf(c.randomMin.toFloat()..c.randomMax.toFloat()) }
                            RangeSlider(
                                value = range, onValueChange = { range = it }, valueRange = 5f..120f, steps = 22,
                                onValueChangeFinished = { vm.update { it.copy(randomMin = range.start.toInt(), randomMax = range.endInclusive.toInt()) } },
                                colors = SliderDefaults.colors(thumbColor = Zen.Pine2, activeTrackColor = Zen.Pine2, inactiveTrackColor = Zen.Line,
                                    activeTickColor = Zen.Pine2.copy(alpha = 0f), inactiveTickColor = Zen.Line.copy(alpha = 0f)),
                            )
                            Row(Modifier.fillMaxWidth()) {
                                Text("5", style = ZenType.Small, modifier = Modifier.weight(1f))
                                Text("120", style = ZenType.Small)
                            }
                        }
                        BellMode.INTERVAL -> {
                            Text(stringResource(R.string.bell_interval_desc), style = ZenType.Muted, modifier = Modifier.padding(bottom = 12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(20, 30, 45, 60, 90).forEach { m ->
                                    PChip(androidx.compose.ui.res.pluralStringResource(R.plurals.minutes_short, m, m), c.intervalMin == m) {
                                        vm.update { it.copy(intervalMin = m) }
                                    }
                                }
                            }
                        }
                        BellMode.TIMES -> {
                            Text(stringResource(R.string.bell_times_desc), style = ZenType.Muted, modifier = Modifier.padding(bottom = 12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                c.times.forEach { t ->
                                    val removeLabel = stringResource(R.string.bell_remove_time, hhmm(t))
                                    Row(
                                        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Zen.Surface)
                                            .border(1.dp, Zen.Line, RoundedCornerShape(18.dp)).padding(start = 14.dp, end = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(hhmm(t), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Zen.Ink)
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            Modifier.size(24.dp).clip(CircleShape).semantics { contentDescription = removeLabel }
                                                .tap { vm.update { it.copy(times = it.times - t) } },
                                            contentAlignment = Alignment.Center,
                                        ) { ZIconView(ZIcons.Close, Zen.Faint, size = 14.dp) }
                                    }
                                }
                                Box(
                                    Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).border(1.5.dp, Zen.Ice3, RoundedCornerShape(18.dp))
                                        .tap { pickTime(12 * 60) { m -> vm.update { it.copy(times = (it.times + m).distinct().sorted()) } } }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center,
                                ) { Text(stringResource(R.string.bell_add_time), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, color = Zen.Pine) }
                            }
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.bell_quiet))
            ZenCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 14.dp)) {
                        Text(stringResource(R.string.bell_quiet_title), style = ZenType.Strong)
                        Text(stringResource(R.string.bell_quiet_desc), style = ZenType.Muted)
                    }
                    ZenSwitch(c.quietEnabled, label = stringResource(R.string.bell_quiet)) { on -> vm.update { it.copy(quietEnabled = on) } }
                }
                Row(Modifier.padding(top = 14.dp).alpha(if (c.quietEnabled) 1f else .45f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeBox(stringResource(R.string.bell_from), c.quietFrom, Modifier.weight(1f)) {
                        pickTime(c.quietFrom) { m -> vm.update { it.copy(quietFrom = m) } }
                    }
                    TimeBox(stringResource(R.string.bell_until), c.quietUntil, Modifier.weight(1f)) {
                        pickTime(c.quietUntil) { m -> vm.update { it.copy(quietUntil = m) } }
                    }
                }
            }
        }

        ZenButton(stringResource(R.string.bell_preview), Modifier.fillMaxWidth().padding(top = 16.dp), BtnStyle.Line, icon = ZIcons.Play) {
            vm.preview(); toast(previewMsg)
            scope.launch {
                listOf(14f, -11f, 7f, -3f, 0f).forEach { swing.animateTo(it, tween(240)) }
            }
        }

        SectionHeader(stringResource(R.string.bell_recent))
        ZenCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
            if (history.isEmpty()) Text(stringResource(R.string.bell_no_history), style = ZenType.Muted, modifier = Modifier.padding(vertical = 14.dp))
            history.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(color = Zen.Line)
                HistoryRow(e) { noteFor = e }
            }
        }
    }

    noteFor?.let { e ->
        var text by remember(e.id) { mutableStateOf(e.note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { noteFor = null },
            containerColor = Zen.Surface,
            title = { Text(stringResource(R.string.bell_add_note), style = ZenType.Strong) },
            text = {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Zen.Surface2).border(1.dp, Zen.Line, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    if (text.isEmpty()) Text(stringResource(R.string.bell_note_hint), fontFamily = Inter, fontSize = 15.sp, color = Zen.Faint)
                    BasicTextField(text, { if (it.length <= 120) text = it }, textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Zen.Ink),
                        cursorBrush = SolidColor(Zen.Pine), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton({ if (text.isNotBlank()) vm.note(e.id, text.trim()); noteFor = null }) {
                    Text(stringResource(R.string.save), color = Zen.Pine, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton({ noteFor = null }) { Text(stringResource(R.string.back), color = Zen.Muted, fontFamily = Inter) } },
        )
    }
}

@Composable
private fun nextLabel(c: BellConfig, nextAt: Long?): String {
    if (!c.enabled) return stringResource(R.string.bell_off)
    if (nextAt == null) return stringResource(R.string.bell_add_time_first)
    val minutes = ((nextAt - System.currentTimeMillis()) / 60000).toInt().coerceAtLeast(1)
    return if (c.mode == BellMode.RANDOM && minutes < 180) stringResource(R.string.bell_next_in, minutes)
    else stringResource(R.string.bell_next_at, timeOf(nextAt))
}

@Composable
private fun TimeBox(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Zen.Surface2).border(1.dp, Zen.Line, RoundedCornerShape(14.dp))
            .tap(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, fontFamily = Inter, fontSize = 11.5.sp, color = Zen.Faint)
        Text(hhmm(minutes), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 18.sp, color = Zen.Pine, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun HistoryRow(e: BellEvent, onNote: () -> Unit) {
    val dt = e.rangAt.toLocalDateTime()
    val day = when (dt.toLocalDate()) {
        LocalDate.now() -> ""
        LocalDate.now().minusDays(1) -> stringResource(R.string.yesterday) + " "
        else -> "${dt.toLocalDate()} "
    }
    Row(Modifier.fillMaxWidth().tap(onClick = onNote).padding(horizontal = 4.dp, vertical = 14.dp)) {
        Text(timeOf(e.rangAt), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Zen.Pine, modifier = Modifier.width(54.dp))
        Column(Modifier.weight(1f)) {
            val status = stringResource(if (e.answered) R.string.bell_paused else R.string.bell_missed)
            Text(
                e.note ?: status, fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp,
                color = if (e.note == null && !e.answered) Zen.Faint else Zen.Ink,
                fontStyle = if (e.note == null && !e.answered) FontStyle.Italic else FontStyle.Normal,
            )
            Text(
                day + if (e.note == null) stringResource(R.string.bell_add_note) else status,
                fontFamily = Inter, fontSize = 12.sp, color = Zen.Faint, modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
