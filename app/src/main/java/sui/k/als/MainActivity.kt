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
import sui.k.als.boot.BootScreen
import sui.k.als.boot.Splash
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYScreen

val localAppFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

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
            var lastActiveSession by remember { mutableStateOf<TTYInstance?>(null) }

            CompositionLocalProvider(localAppFont provides remember {
                runCatching { FontFamily(Font("fonts/GoogleSansFlex.ttf", assets)) }.getOrDefault(FontFamily.Default)
            }) {
                when (switch) {
                    "splash" -> Splash(onTimeout = { switch = "boot" })
                    "boot" -> BootScreen {
                        // 这里不再跳转到 terminal 分支，因为 BootScreen 内部已经接管了 TTY 显示逻辑
                        // 如果你点击 Exit 触发 onFinished，这里可以处理 App 退出逻辑
                    }
                    "terminal" -> {
                        // 如果你仍需保留这个分支，必须传入一个有效的实例
                        lastActiveSession?.let { TTYScreen(it) }
                    }
                }
            }
        }
    }
}