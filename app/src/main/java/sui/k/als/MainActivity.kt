package sui.k.als

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import sui.k.als.boot.Hub
import sui.k.als.boot.Splash

val localFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                Box {
                    Hub { finish() }
                    if (showSplash) Splash { showSplash = false }
                }
            }
        }
    }
}
