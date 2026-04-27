package sui.k.als.chr.qcom
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sui.k.als.R
import sui.k.als.alsPath
import sui.k.als.localFont
import sui.k.als.su
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import sui.k.als.tty.ttySession
import sui.k.als.ui.ALSButton
import java.io.File
const val chrPath = "$alsPath/app/chr"
private var term1Instance: TTYInstance? = null
private var term2Instances = mutableMapOf<String, TTYInstance>()
@Composable
fun Chr(onTTYCreated: (TTYInstance) -> Unit, scope: CoroutineScope) {
    val context = LocalContext.current
    var configs by remember { mutableStateOf(emptyList<Map<String, Any>>()) }
    fun refresh() {
        val directory = File(chrPath).apply { if (!exists()) mkdirs() }
        val directoryList = directory.listFiles { file -> file.isDirectory }?.map { subDir ->
            mutableMapOf<String, Any>("name" to subDir.name)
        } ?: emptyList()
        configs = directoryList.ifEmpty { listOf(mapOf("name" to "Debian 13.1")) }
    }
    LaunchedEffect(Unit) { refresh() }
    val startTTY: (String?, suspend (TTYInstance) -> Unit) -> Unit = { key, onRun ->
        var localInstance: TTYInstance? = null
        val sessionStub = object : TTYSessionStub() {
            override fun onSessionFinished(session: TerminalSession) {
                if (key == null) term1Instance = null
                else term2Instances.remove(key)
            }
        }
        val viewStub = object : TTYViewStub() {
            override fun onSingleTapUp(event: MotionEvent) {
                localInstance?.view?.requestFocus()
                context.getSystemService(InputMethodManager::class.java)?.showSoftInput(localInstance?.view, 0)
            }
        }
        localInstance = createTTYInstance(context, sessionStub, viewStub)
        if (key == null) term1Instance = localInstance
        else term2Instances[key] = localInstance
        onTTYCreated(localInstance)
        scope.launch {
            delay(90)
            onRun(localInstance)
        }
    }
    val showTerm1 = {
        if (term1Instance != null) {
            ttySession = term1Instance!!.session
            onTTYCreated(term1Instance!!)
        } else startTTY(null) { instance ->
            ttySession = instance.session
            cmd(su)
            cmd("cd $alsPath && txc")
            cmd("""pkg install x11-repo -y && pkg install pulseaudio termux-x11 -y && su -c “am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.termux/.app.TermuxActivity && sleep 3 && input text "termux-x11 :1 -dpi 315" && input keyevent 66"""")}
    }
    val showTerm2 = { config: Map<String, Any> ->
        val configName = config["name"].toString()
        if (term2Instances.containsKey(configName)) {
            val instance = term2Instances[configName]!!
            ttySession = instance.session
            onTTYCreated(instance)
        } else startTTY(configName) { instance ->
            ttySession = instance.session
            cmd(su)
            cmd("cd $alsPath")
            val mountPath = "$chrPath/$configName"
            cmd($$"CHROOT_PATH=\"$$mountPath\"; grep -q \"$CHROOT_PATH/sys\" /proc/mounts || mount -t sysfs sys \"$CHROOT_PATH/sys\"; grep -q \"$CHROOT_PATH/proc\" /proc/mounts || mount -t proc proc \"$CHROOT_PATH/proc\"; grep -q \"$CHROOT_PATH/dev\" /proc/mounts || mount -o bind /dev \"$CHROOT_PATH/dev\"; grep -q \"$CHROOT_PATH/dev/pts\" /proc/mounts || mount -t devpts devpts \"$CHROOT_PATH/dev/pts\"; chroot \"$CHROOT_PATH\" /usr/bin/env -i HOME=/root TERM=\"$TERM\" DISPLAY=:1 LANG=zh_CN.UTF-8 LC_ALL=zh_CN.UTF-8 PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /bin/bash --login")
            cmd("bash up")
        }
    }
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(configs) { configItem ->
                ChrRow(
                    name = configItem["name"].toString(),
                    onTerm1 = { showTerm1() },
                    onTerm2 = { showTerm2(configItem) }
                )
            }
        }
    }
}
@Composable
fun ChrRow(name: String, onTerm1: () -> Unit, onTerm2: () -> Unit) =
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Color(0xFF111111)).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = localFont.current
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ALSButton(R.drawable.terminal) { onTerm1() }
            ALSButton(R.drawable.terminal) { onTerm2() }
        }
    }