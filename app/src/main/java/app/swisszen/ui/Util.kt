package app.swisszen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import app.swisszen.AppContainer
import app.swisszen.SwissZenApp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** "1:05" */
fun mmss(seconds: Double): String { val s = seconds.toInt().coerceAtLeast(0); return "${s / 60}:${"%02d".format(s % 60)}" }

/** "01:05" */
fun mmss2(seconds: Int): String = "%02d:%02d".format(seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)

fun Long.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
fun LocalDate.startMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun hhmm(minuteOfDay: Int) = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
fun timeOf(ms: Long): String = ms.toLocalDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
fun dayLabel(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern(if (locale.language == "ru") "EE, d MMM" else "EEE, d MMM", locale))
fun mediumDate(date: LocalDate, locale: Locale): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** Streak of consecutive days (ending today, or yesterday if today is still empty) that have activity. */
fun streak(days: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
    var d = if (today in days) today else today.minusDays(1)
    var n = 0
    while (d in days) { n++; d = d.minusDays(1) }
    return n
}

/** Builds a ViewModel that needs the app container. */
inline fun <reified VM : ViewModel> containerFactory(crossinline make: (AppContainer) -> VM) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SwissZenApp
        @Suppress("UNCHECKED_CAST") return make(app.container) as T
    }
}

/** Keeps the screen on while this composable is shown (breathing sessions, meditations). */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
