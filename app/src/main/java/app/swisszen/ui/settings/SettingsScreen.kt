package app.swisszen.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import app.swisszen.data.AppSettings
import app.swisszen.ui.Language
import app.swisszen.ui.ThemeMode
import app.swisszen.ui.components.RoundIconButton
import app.swisszen.ui.components.SectionHeader
import app.swisszen.ui.components.ZIcons
import app.swisszen.ui.components.ZenCard
import app.swisszen.ui.components.ZenSwitch
import app.swisszen.ui.components.tap
import app.swisszen.ui.containerFactory
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun sound(on: Boolean) = viewModelScope.launch { c.settings.setSound(on) }
    fun haptics(on: Boolean) = viewModelScope.launch { c.settings.setHaptics(on) }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = containerFactory { SettingsViewModel(it) })
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var lang by remember { mutableStateOf(Language.current()) }
    var theme by remember { mutableStateOf(ThemeMode.current()) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    BackHandler(onBack = onBack)

    Column(
        Modifier.fillMaxSize().background(Zen.Bg).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(ZIcons.Back, stringResource(R.string.back), onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.settings_title), style = ZenType.Title)
        }

        SectionHeader(stringResource(R.string.settings_language))
        RadioCard(
            listOf(Language.SYSTEM to stringResource(R.string.lang_system), Language.EN to stringResource(R.string.lang_en), Language.RU to stringResource(R.string.lang_ru)),
            lang,
        ) { lang = it; Language.set(it) } // recreates the activity in the new language

        SectionHeader(stringResource(R.string.settings_theme))
        RadioCard(
            listOf(ThemeMode.SYSTEM to stringResource(R.string.theme_system), ThemeMode.LIGHT to stringResource(R.string.theme_light), ThemeMode.DARK to stringResource(R.string.theme_dark)),
            theme,
        ) { theme = it; ThemeMode.set(context, it) }

        SectionHeader(stringResource(R.string.settings_feedback))
        ZenCard(Modifier.fillMaxWidth()) {
            ToggleRow(stringResource(R.string.settings_sound), stringResource(R.string.settings_sound_desc), s.sound, vm::sound)
            HorizontalDivider(color = Zen.Line, modifier = Modifier.padding(vertical = 14.dp))
            ToggleRow(stringResource(R.string.settings_haptics), stringResource(R.string.settings_haptics_desc), s.haptics, vm::haptics)
        }

        SectionHeader(stringResource(R.string.settings_notifications))
        ZenCard(Modifier.fillMaxWidth()) {
            Text(
                stringResource(if (BellAlarms.canNotify(context)) R.string.settings_notifications_on else R.string.settings_notifications_off),
                style = ZenType.Muted,
            )
        }

        SectionHeader(stringResource(R.string.settings_about))
        ZenCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_about_text, version), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Zen.Ink)
            Text(stringResource(R.string.settings_disclaimer), style = ZenType.Muted, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun <T> RadioCard(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    ZenCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        options.forEachIndexed { i, (value, label) ->
            if (i > 0) HorizontalDivider(color = Zen.Line)
            val on = selected == value
            Row(
                Modifier.fillMaxWidth().semantics { this.selected = on }.tap(role = Role.RadioButton) { onSelect(value) }
                    .padding(vertical = 15.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = ZenType.Body, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (on) Zen.Pine else Zen.Line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (on) Box(Modifier.size(10.dp).background(Zen.Pine, CircleShape)) }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(title, style = ZenType.Strong)
            Text(desc, style = ZenType.Muted)
        }
        ZenSwitch(checked, label = title, onChange = onChange)
    }
}
