package app.swisszen

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import app.swisszen.bell.BellAlarms
import app.swisszen.ui.SwissZenRoot
import app.swisszen.ui.theme.SwissZenTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {
    /** Tab requested from outside (e.g. tapping the bell notification). */
    private val openRequest = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        setContent {
            SwissZenTheme {
                SwissZenRoot(openRequest = openRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra(BellAlarms.EXTRA_OPEN)?.let {
            openRequest.value = it
            BellAlarms.dismiss(this)
        }
    }
}
