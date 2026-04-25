package sui.k.als

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

val localFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            val font = remember {
                runCatching {
                    FontFamily(
                        Font(
                            "font/GoogleSansFlex.ttf", assets
                        )
                    )
                }.getOrDefault(FontFamily.Default)
            }
            var showSplash by rememberSaveable { mutableStateOf(true) }
            CompositionLocalProvider(localFont provides font) {
                if (showSplash) {
                    Splash { showSplash = false }
                } else {
                    Hub { finish() }
                }
            }
        }
    }
}