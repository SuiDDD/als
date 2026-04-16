package sui.k.als.vm
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onEditorExit: () -> Unit
) {
    if (showType) TypeSelectDialog(onDismissType, onSelectQvm, onSelectCvm)
    Surface(color = Color.White) {
        Box(Modifier.fillMaxSize()) {
            when {
                terminal != null -> TTYScreen(terminal)
                editing != null -> {
                    val command = editing.raw?.optString("command", "") ?: ""
                    if (command.contains("crosvm")) CVMCreate(editing, onEditorExit) else QVMCreate(editing, onEditorExit)
                }
                creatingType == "qemu" -> QVMCreate(null, onEditorExit)
                creatingType == "crosvm" -> CVMCreate(null, onEditorExit)
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Spacer(Modifier.fillMaxHeight(0.018f))
                        VMTopBar(onCreateClick)
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(configs, { it.name }) { config ->
                                VMCard(config, onEditVM, onStartVM)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun VMTopBar(onCreateClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Row(
        Modifier.fillMaxWidth().height(18.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Text("Virtual Machines", fontSize = 9.sp, fontFamily = localFont.current, color = Color.Black)
        Box(
            Modifier.size(18.dp).clickable(interaction, null, onClick = onCreateClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.add_wght300_24px),
                null,
                tint = if (isPressed) Color(0xFFE95420) else Color.Black,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
@Composable
fun VMCard(
    config: VMConfig, onEdit: (VMConfig) -> Unit, onStart: (VMConfig) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val ubuntuOrange = Color(0xFFE95420)
    Row(
        Modifier.fillMaxWidth().height(18.dp).clickable(interaction, null) { onEdit(config) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    config.name,
                    fontSize = 9.sp,
                    fontFamily = localFont.current,
                    color = if (isPressed) ubuntuOrange else Color.Black
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    if (config.isRunning) "RUN" else "STOP",
                    fontSize = 9.sp,
                    fontFamily = localFont.current,
                    color = if (config.isRunning) Color(0xFF2E7D32) else Color.Gray.copy(0.4f)
                )
            }
        }
        Box(
            Modifier.size(18.dp).clickable { onStart(config) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.play_arrow_wght300_24px),
                null,
                tint = ubuntuOrange,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
@Composable
fun TypeSelectDialog(
    onDismiss: () -> Unit, onSelectQvm: () -> Unit, onSelectCvm: () -> Unit
) = Dialog(onDismissRequest = onDismiss) {
    Surface(color = Color.White) {
        Column(Modifier.width(180.dp)) {
            Box(Modifier.fillMaxWidth().height(18.dp), Alignment.CenterStart) {
                Text("Select VMM", fontSize = 9.sp, fontFamily = localFont.current, color = Color.Gray)
            }
            listOf("QEMU Gunyah" to onSelectQvm, "crosvm" to onSelectCvm).forEach { (label, onClick) ->
                val interaction = remember { MutableInteractionSource() }
                val isPressed by interaction.collectIsPressedAsState()
                Box(
                    Modifier.fillMaxWidth().height(18.dp).clickable(interaction, null) { onClick() },
                    Alignment.CenterStart
                ) {
                    Text(
                        label,
                        fontSize = 9.sp,
                        fontFamily = localFont.current,
                        color = if (isPressed) Color(0xFFE95420) else Color.Black
                    )
                }
            }
        }
    }
}