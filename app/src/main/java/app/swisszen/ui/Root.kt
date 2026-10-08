package app.swisszen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.swisszen.R
import app.swisszen.ui.bell.BellScreen
import app.swisszen.ui.breathe.BreatheScreen
import app.swisszen.ui.breathe.WimHofSessionScreen
import app.swisszen.ui.components.ZIcon
import app.swisszen.ui.components.ZIconView
import app.swisszen.ui.components.ZIcons
import app.swisszen.ui.components.tap
import app.swisszen.ui.home.HomeScreen
import app.swisszen.ui.journal.JournalScreen
import app.swisszen.ui.practices.MeditationPlayerScreen
import app.swisszen.ui.practices.PracticeSessionScreen
import app.swisszen.ui.practices.PracticesScreen
import app.swisszen.ui.settings.SettingsScreen
import app.swisszen.ui.theme.Inter
import app.swisszen.ui.theme.Zen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/** Show a short message at the bottom of the screen. */
val LocalToast = staticCompositionLocalOf<(String) -> Unit> { {} }

object Routes {
    const val HOME = "home"
    const val BREATHE = "breathe"
    const val BELL = "bell"
    const val PRACTICES = "practices"
    const val JOURNAL = "journal"
    const val SETTINGS = "settings"
    const val WIM_HOF = "wimhof"
    const val PRACTICE = "practice/{i}"
    const val MEDITATION = "meditation/{i}"
}

private data class Tab(val route: String, val label: Int, val icon: ZIcon)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.tab_home, ZIcons.Knife),
    Tab(Routes.BREATHE, R.string.tab_breathe, ZIcons.Breathe),
    Tab(Routes.BELL, R.string.tab_bell, ZIcons.Bell),
    Tab(Routes.PRACTICES, R.string.tab_practices, ZIcons.Lotus),
    Tab(Routes.JOURNAL, R.string.tab_journal, ZIcons.Pencil),
)

fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun SwissZenRoot(openRequest: MutableStateFlow<String?>) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var toast by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val request by openRequest.collectAsState()
    LaunchedEffect(request) {
        request?.let { nav.goTab(it); openRequest.value = null }
    }
    LaunchedEffect(toast) {
        if (toast != null) { delay(2000); toast = null }
    }

    CompositionLocalProvider(LocalToast provides { msg -> toast = msg to System.nanoTime() }) {
        Box(Modifier.fillMaxSize().background(Zen.Bg)) {
            Column(Modifier.fillMaxSize()) {
                NavHost(
                    nav, startDestination = Routes.HOME, modifier = Modifier.weight(1f),
                    enterTransition = { fadeIn() + slideInVertically { it / 40 } },
                    exitTransition = { fadeOut() },
                ) {
                    composable(Routes.HOME) { HomeScreen(onOpen = { nav.goTab(it) }, onSettings = { nav.navigate(Routes.SETTINGS) }) }
                    composable(Routes.BREATHE) { BreatheScreen(onStartWimHof = { nav.navigate(Routes.WIM_HOF) }) }
                    composable(Routes.BELL) { BellScreen() }
                    composable(Routes.PRACTICES) {
                        PracticesScreen(
                            onPractice = { nav.navigate("practice/$it") },
                            onMeditation = { nav.navigate("meditation/$it") },
                        )
                    }
                    composable(Routes.JOURNAL) { JournalScreen() }
                    composable(Routes.SETTINGS,
                        enterTransition = { slideInHorizontally { it } }, exitTransition = { slideOutHorizontally { it } }) {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                    composable(Routes.WIM_HOF,
                        enterTransition = { slideInVertically { it } }, exitTransition = { slideOutVertically { it } }) {
                        WimHofSessionScreen(onClose = { nav.popBackStack() })
                    }
                    composable(Routes.PRACTICE, listOf(navArgument("i") { type = NavType.IntType }),
                        enterTransition = { slideInHorizontally { it } }, exitTransition = { slideOutHorizontally { it } }) {
                        PracticeSessionScreen(it.arguments?.getInt("i") ?: 0, onClose = { nav.popBackStack() })
                    }
                    composable(Routes.MEDITATION, listOf(navArgument("i") { type = NavType.IntType }),
                        enterTransition = { slideInHorizontally { it } }, exitTransition = { slideOutHorizontally { it } }) {
                        MeditationPlayerScreen(it.arguments?.getInt("i") ?: 0, onClose = { nav.popBackStack() })
                    }
                }
                if (tabs.any { it.route == route }) TabBar(route) { nav.goTab(it) }
            }

            AnimatedVisibility(
                toast != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 104.dp),
            ) {
                Text(
                    toast?.first.orEmpty(),
                    color = Zen.OnPine, fontFamily = Inter, fontSize = 13.5.sp,
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Zen.Pine)
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                )
            }
        }
    }
}

@Composable
private fun TabBar(current: String?, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Zen.Surface2).navigationBarsPadding()) {
        HorizontalDivider(color = Zen.Line, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            tabs.forEach { tab ->
                val on = tab.route == current
                val dot by animateFloatAsState(if (on) 1f else 0f, label = "tabDot")
                Column(
                    Modifier
                        .weight(1f)
                        .semantics { selected = on }
                        .tap(role = Role.Tab) { onSelect(tab.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(58.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Zen.Ice else Zen.Surface2.copy(alpha = 0f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        ZIconView(tab.icon, if (on) Zen.Pine else Zen.Muted, size = 22.dp)
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = (-9).dp, y = 3.dp).size(6.dp).scale(dot)
                                .background(Zen.Red, CircleShape)
                        )
                    }
                    Text(
                        stringResource(tab.label), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.5.sp,
                        color = if (on) Zen.Pine else Zen.Muted, maxLines = 1, modifier = Modifier.padding(top = 4.dp).height(16.dp),
                    )
                }
            }
        }
    }
}
