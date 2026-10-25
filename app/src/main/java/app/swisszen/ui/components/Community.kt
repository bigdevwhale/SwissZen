package app.swisszen.ui.components

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.swisszen.R
import app.swisszen.ui.theme.Zen
import app.swisszen.ui.theme.ZenType

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "swisszen"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

/** Report a bug, suggest an idea, follow updates, join the chat; each opens Telegram. */
@Composable
fun CommunityLinks() {
    val ctx = LocalContext.current
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    SectionHeader(stringResource(R.string.community_title))
    ZenCard(Modifier.fillMaxWidth(), padding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp)) {
        Text(stringResource(R.string.community_sub), style = ZenType.Muted, modifier = Modifier.padding(bottom = 6.dp))
        listOf(
            Link("🐞", R.string.community_bug, R.string.community_bug_sub, Community.BUG),
            Link("💡", R.string.community_idea, R.string.community_idea_sub, Community.IDEA),
            Link("🔔", R.string.community_follow, R.string.community_follow_sub, Community.FOLLOW),
            Link("💬", R.string.community_chat, R.string.community_chat_sub, Community.CHAT),
        ).forEachIndexed { i, (icon, label, sub, url) ->
            if (i > 0) HorizontalDivider(color = Zen.Line)
            Row(Modifier.fillMaxWidth().tap { open(url) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 18.sp, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(label), style = ZenType.Strong)
                    Text(stringResource(sub), style = ZenType.Muted)
                }
                Text("›", style = ZenType.Title.copy(fontSize = 22.sp, color = Zen.Pine))
            }
        }
    }
}

private data class Link(val icon: String, @param:StringRes val label: Int, @param:StringRes val sub: Int, val url: String)
