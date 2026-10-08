package app.swisszen.ui.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.swisszen.AppContainer
import app.swisszen.R
import app.swisszen.data.JournalEntry
import app.swisszen.ui.LocalToast
import app.swisszen.ui.components.AppBar
import app.swisszen.ui.components.BtnStyle
import app.swisszen.ui.components.SectionHeader
import app.swisszen.ui.components.ZIconView
import app.swisszen.ui.components.ZIcons
import app.swisszen.ui.components.ZenButton
import app.swisszen.ui.components.ZenCard
import app.swisszen.ui.components.tap
import app.swisszen.ui.components.zenShadow
import app.swisszen.ui.containerFactory
import app.swisszen.ui.currentLocale
import app.swisszen.ui.dayLabel
import app.swisszen.ui.startMillis
import app.swisszen.ui.streak
import app.swisszen.ui.timeOf
import app.swisszen.ui.toLocalDateTime
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class JournalViewModel(private val c: AppContainer) : ViewModel() {
    val recent = c.db.journal().recent(30).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val days = c.db.journal().allTimes().map { t -> t.map { it.toLocalDateTime().toLocalDate() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val echo = MutableStateFlow<JournalEntry?>(null)

    init {
        viewModelScope.launch {
            val today = LocalDate.now()
            echo.value = c.db.journal().randomBetween(today.minusDays(42).startMillis(), today.minusDays(13).startMillis())
        }
    }

    fun save(text: String) = viewModelScope.launch {
        c.db.journal().insert(JournalEntry(createdAt = System.currentTimeMillis(), text = text))
    }
}

private const val MAX = 120

@Composable
fun JournalScreen() {
    val vm: JournalViewModel = viewModel(factory = containerFactory { JournalViewModel(it) })
    val entries by vm.recent.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    val echo by vm.echo.collectAsStateWithLifecycle()
    val prompts = stringArrayResource(R.array.journal_prompts)
    var promptIdx by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfYear % prompts.size) }
    var text by rememberSaveable { mutableStateOf("") }
    val toast = LocalToast.current
    val focus = LocalFocusManager.current
    val locale = currentLocale()
    val writeFirst = stringResource(R.string.journal_write_first)
    val saved = stringResource(R.string.journal_saved)
    val today = LocalDate.now()

    fun submit() {
        val v = text.trim()
        if (v.isEmpty()) { toast(writeFirst); return }
        vm.save(v); text = ""; focus.clearFocus(); toast(saved)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 28.dp)
    ) {
        AppBar(stringResource(R.string.journal_title)) { Text(dayLabel(today, locale), style = ZenType.Muted) }

        // Prompt card
        Column(
            Modifier.fillMaxWidth().padding(top = 18.dp).clip(Zen.RLg)
                .background(Brush.radialGradient(listOf(Color(0xFF2F5E4C), Zen.Pine), radius = 900f, center = androidx.compose.ui.geometry.Offset(900f, 0f)))
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp)
        ) {
            Text(stringResource(R.string.todays_prompt).uppercase(), style = ZenType.Eyebrow.copy(color = Zen.Ice2))
            AnimatedContent(promptIdx, label = "prompt", transitionSpec = { fadeIn() togetherWith fadeOut() }) { i ->
                Text(prompts[i], fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 23.sp, lineHeight = 29.sp, letterSpacing = (-0.02).em,
                    color = Zen.OnPine, modifier = Modifier.padding(top = 10.dp, bottom = 16.dp))
            }
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(Zen.Ice.copy(alpha = .14f)).tap { promptIdx = (promptIdx + 1) % prompts.size }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZIconView(ZIcons.Shuffle, Zen.Ice, size = 16.dp, strokeWidth = 1.7f)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.another_prompt), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Zen.Ice)
            }
        }

        // One-line entry
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).zenShadow(RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Zen.Surface)
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) Text(stringResource(R.string.journal_placeholder), fontFamily = Inter, fontSize = 15.sp, color = Zen.Faint)
                BasicTextField(
                    text, { if (it.length <= MAX) text = it.replace('\n', ' ') },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Zen.Ink),
                    cursorBrush = SolidColor(Zen.Pine),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ZenButton(stringResource(R.string.save), style = BtnStyle.Red, height = 44.dp, onClick = ::submit)
        }
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp)) {
            Text(stringResource(R.string.journal_rule), style = ZenType.Small, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.journal_count, text.length, MAX), style = ZenType.Small)
        }

        // Streak + this week
        val n = streak(days, today)
        ZenCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$n", fontFamily = Inter, fontWeight = FontWeight.Light, fontSize = 40.sp, letterSpacing = (-0.04).em, color = Zen.Pine)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.journal_day_streak), style = ZenType.Strong)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (6 downTo 0).forEach { back ->
                            val d = today.minusDays(back.toLong())
                            val fill = d in days
                            Box(
                                Modifier.size(26.dp)
                                    .then(if (back == 0) Modifier.border(1.5.dp, Zen.Pine, CircleShape).padding(3.dp) else Modifier)
                                    .clip(CircleShape).background(if (fill) Zen.Pine else Zen.PineSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale), fontFamily = Inter, fontWeight = FontWeight.SemiBold,
                                    fontSize = if (back == 0) 9.sp else 10.5.sp, color = if (fill) Zen.OnPine else Zen.Pine)
                            }
                        }
                    }
                }
            }
        }

        // Echo from a past week
        echo?.let { e ->
            SectionHeader(stringResource(R.string.journal_echo))
            val date = e.createdAt.toLocalDateTime().toLocalDate()
            val weeks = (ChronoUnit.DAYS.between(date, today) / 7).toInt().coerceAtLeast(1)
            Column(
                Modifier.fillMaxWidth().clip(Zen.RLg).background(Brush.linearGradient(listOf(Zen.Ice, Color(0xFFEDF4F7)))).padding(20.dp)
            ) {
                Text("“${e.text}”", fontFamily = Inter, fontSize = 17.sp, lineHeight = 25.sp, letterSpacing = (-0.01).em, color = Zen.Ink)
                Text(stringResource(R.string.journal_echo_when, dayLabel(date, locale), pluralStringResource(R.plurals.weeks_ago, weeks, weeks)),
                    fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, color = Zen.Ice3, modifier = Modifier.padding(top = 12.dp))
            }
        }

        // Recent lines
        SectionHeader(stringResource(R.string.journal_recent))
        ZenCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            if (entries.isEmpty()) Text(stringResource(R.string.journal_empty), style = ZenType.Muted, modifier = Modifier.padding(vertical = 14.dp))
            entries.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(color = Zen.Line)
                val date = e.createdAt.toLocalDateTime().toLocalDate()
                Column(Modifier.padding(horizontal = 4.dp, vertical = 14.dp)) {
                    Text(e.text, style = ZenType.Body)
                    Text(
                        if (date == today) stringResource(R.string.journal_today_at, timeOf(e.createdAt)) else dayLabel(date, locale),
                        style = ZenType.Small, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
