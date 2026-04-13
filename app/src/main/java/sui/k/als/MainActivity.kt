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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import sui.k.als.boot.Home
import sui.k.als.boot.Splash
val localFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        setContent {
            var switch by rememberSaveable { mutableStateOf("splash") }
            CompositionLocalProvider(localFont provides remember {
                runCatching { FontFamily(Font("fonts/GoogleSansFlex.ttf", assets)) }.getOrDefault(
                    FontFamily.Default
                )
            }) {
                when (switch) {
                    "splash" -> Splash(onTimeout = { switch = "home" })
                    "home" -> Home { finish() }
                }
            }
        }
    }
}