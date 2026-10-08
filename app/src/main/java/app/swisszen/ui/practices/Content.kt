package app.swisszen.ui.practices

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import app.swisszen.R
import app.swisszen.ui.components.ZIcon
import app.swisszen.ui.components.ZIcons

/** Step-by-step micro-practice; [numbered] shows the step's countdown digit in the glyph (5-4-3-2-1). */
data class Practice(
    val id: String,
    @StringRes val name: Int,
    @StringRes val desc: Int,
    val minutes: Int,
    val icon: ZIcon,
    @ArrayRes val heads: Int,
    @ArrayRes val hints: Int,
    val numbered: List<String>? = null,
)

/** Timed meditation; phase titles/hints come from string arrays, [seconds] per phase. */
data class Meditation(
    val id: String,
    @StringRes val name: Int,
    @StringRes val desc: Int,
    val icon: ZIcon,
    @ArrayRes val titles: Int,
    @ArrayRes val hints: Int,
    val seconds: List<Int>,
) {
    val minutes get() = seconds.sum() / 60
}

object Catalog {
    val practices = listOf(
        Practice("grounding", R.string.p_grounding, R.string.p_grounding_desc, 3, ZIcons.Ground,
            R.array.p_grounding_heads, R.array.p_grounding_hints, listOf("5", "4", "3", "2", "1", "·")),
        Practice("scan", R.string.p_scan, R.string.p_scan_desc, 5, ZIcons.Body, R.array.p_scan_heads, R.array.p_scan_hints),
        Practice("three", R.string.p_three, R.string.p_three_desc, 1, ZIcons.Eye,
            R.array.p_three_heads, R.array.p_three_hints, listOf("1", "2", "3")),
        Practice("start", R.string.p_start, R.string.p_start_desc, 2, ZIcons.Sunrise, R.array.p_start_heads, R.array.p_start_hints),
        Practice("gratitude", R.string.p_gratitude, R.string.p_gratitude_desc, 1, ZIcons.Heart, R.array.p_gratitude_heads, R.array.p_gratitude_hints),
    )

    val meditations = listOf(
        Meditation("anchor", R.string.m_anchor, R.string.m_anchor_desc, ZIcons.Wave,
            R.array.m_anchor_titles, R.array.m_anchor_hints, listOf(40, 90, 100, 40, 30)),
        Meditation("kindness", R.string.m_kindness, R.string.m_kindness_desc, ZIcons.Heart,
            R.array.m_kindness_titles, R.array.m_kindness_hints, listOf(60, 120, 120, 120, 120, 60)),
        Meditation("mountain", R.string.m_mountain, R.string.m_mountain_desc, ZIcons.Mountain,
            R.array.m_mountain_titles, R.array.m_mountain_hints, listOf(90, 150, 150, 120, 90)),
        Meditation("walking", R.string.m_walking, R.string.m_walking_desc, ZIcons.Steps,
            R.array.m_walking_titles, R.array.m_walking_hints, listOf(60, 180, 60, 300, 180, 120)),
        Meditation("sleep", R.string.m_sleep, R.string.m_sleep_desc, ZIcons.Moon,
            R.array.m_sleep_titles, R.array.m_sleep_hints, listOf(60, 240, 300, 240, 360)),
    )
}
