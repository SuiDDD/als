package sui.k.als.vm
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import sui.k.als.R
import sui.k.als.localFont
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYScreen

@Composable
fun VMContent(
    configs: List<VMConfig>,
    terminal: TTYInstance?,
    creatingType: String?,
    editing: VMConfig?,
    showType: Boolean,
    onStartVM: (VMConfig) -> Unit,
    onEditVM: (VMConfig) -> Unit,
    onCreateClick: () -> Unit,
    onSelectQvm: () -> Unit,
    onSelectCvm: () -> Unit,
    onDismissType: () -> Unit,
    onEditorExit: () -> Unit,
    onTerminalShow: (VMConfig) -> Unit,
    onDisplayShow: (VMConfig) -> Unit
) {
    if (showType) TypeSelectDialog(onDismissType, onSelectQvm, onSelectCvm)
    Surface(color = Color.Black) {
        Box(Modifier.fillMaxSize()) {
            when {
                terminal != null -> TTYScreen(terminal)
                editing != null -> if (editing.raw?.optString("command", "")?.contains("crosvm") == true) CVMCreate(editing, onEditorExit) else QVMCreate(
                    editing,
                    onEditorExit
                )
                creatingType == "qemu" -> QVMCreate(null, onEditorExit)
                creatingType == "crosvm" -> CVMCreate(null, onEditorExit)
                else -> Column(Modifier.fillMaxSize()) {
                    VMTopBar(onCreateClick)
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(configs, { it.name }) { config -> VMCard(config, onEditVM, onStartVM, onTerminalShow, onDisplayShow) }
                    }
                }
            }
        }
    }
}
@Composable
fun VMTopBar(onCreateClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Virtual Machines", fontSize = 10.sp, fontFamily = localFont.current, color = Color.White)
        IconButton(onClick = onCreateClick, modifier = Modifier.size(24.dp)) {
            Icon(painterResource(R.drawable.add_wght300_24px), null, Modifier.size(16.dp), Color.White)
        }
    }
}
@Composable
fun VMCard(config: VMConfig, onEdit: (VMConfig) -> Unit, onStart: (VMConfig) -> Unit, onTerminal: (VMConfig) -> Unit, onDisplay: (VMConfig) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth().height(36.dp).clickable(interaction, null) { onEdit(config) }) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = config.name, fontSize = 10.sp, fontFamily = localFont.current, color = if (isPressed) Color.Gray else Color.White)
                Surface(modifier = Modifier.size(4.dp), shape = CircleShape, color = if (config.isRunning) Color.White else Color.Gray) {}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ onTerminal(config) }, Modifier.size(28.dp)) { Icon(painterResource(R.drawable.terminal_wght300_24px), null, Modifier.size(16.dp), Color.White) }
                if (config.type == "qemu") IconButton({ onDisplay(config) }, Modifier.size(28.dp)) { Icon(painterResource(R.drawable.monitor_wght300_24px), null, Modifier.size(16.dp), Color.White) }
                IconButton({ onStart(config) }, Modifier.size(28.dp)) { Icon(painterResource(R.drawable.play_arrow_wght300_24px), null, Modifier.size(18.dp), Color.White) }
            }
        }
    }
}
@Composable
fun TypeSelectDialog(onDismiss: () -> Unit, onSelectQvm: () -> Unit, onSelectCvm: () -> Unit) = Dialog(onDismissRequest = onDismiss) {
    Surface(color = Color.Black, shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.width(200.dp)) {
            Text(text = "Select VMM", fontSize = 10.sp, color = Color.Gray)
            Text(text = "QEMU Gunyah", modifier = Modifier.fillMaxWidth().clickable { onSelectQvm() }, fontSize = 10.sp, color = Color.White)
            Text(text = "crosvm", modifier = Modifier.fillMaxWidth().clickable { onSelectCvm() }, fontSize = 10.sp, color = Color.White)
        }
    }
}