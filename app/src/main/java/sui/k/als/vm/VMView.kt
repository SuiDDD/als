package sui.k.als.vm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    if (showType) {
        TypeSelectDialog(onDismissType, onSelectQvm, onSelectCvm)
    }
    when {
        terminal != null -> TTYScreen(terminal)
        editing != null || creatingType == "qemu" -> QVMCreate(editing, onEditorExit)
        creatingType == "crosvm" -> CVMCreate(onEditorExit)
        else -> Scaffold(
            containerColor = Color(0xFFF6F6F6),
            topBar = { VMTopBar(onCreateClick) }) { paddingValues ->
            Surface(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                RoundedCornerShape(topStart = 24.dp),
                Color(0xFFF1F1F1)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(configs, { it.name }) { config ->
                        VMCard(config, onEditVM, onStartVM)
                    }
                }
            }
        }
    }
}

@Composable
fun VMTopBar(onCreateClick: () -> Unit) = Row(
    Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 24.dp, vertical = 20.dp),
    Arrangement.SpaceBetween,
    Alignment.CenterVertically
) {
    Text(
        "Virtual Machines",
        fontSize = 16.sp,
        fontFamily = localFont.current,
        fontWeight = FontWeight.Bold
    )
    Surface(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onCreateClick),
        CircleShape,
        Color(0xFFE95420)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.add_wght300_24px), null, tint = Color.White)
        }
    }
}

@Composable
fun VMCard(
    config: VMConfig, onEdit: (VMConfig) -> Unit, onStart: (VMConfig) -> Unit
) = Surface(
    onClick = { onEdit(config) },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = Color.White,
    border = BorderStroke(1.dp, Color.Black.copy(0.03f))
) {
    val f = localFont.current
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(config.name, fontSize = 12.sp, fontFamily = f, fontWeight = FontWeight.SemiBold)
            Text(
                if (config.isRunning) "Running" else "Stopped",
                fontSize = 9.sp,
                fontFamily = f,
                color = if (config.isRunning) Color(0xFF2E7D32) else Color.Gray.copy(0.6f)
            )
        }
        IconButton({ onStart(config) }, Modifier.size(32.dp)) {
            Icon(
                painterResource(R.drawable.play_arrow_wght300_24px), null, tint = Color(0xFFE95420)
            )
        }
    }
}

@Composable
fun TypeSelectDialog(
    onDismiss: () -> Unit, onSelectQvm: () -> Unit, onSelectCvm: () -> Unit
) = Dialog(onDismiss) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFF1F1F1)) {
        Column(
            Modifier
                .padding(24.dp)
                .width(240.dp)
        ) {
            val f = localFont.current
            Text("Select VMM", fontSize = 16.sp, fontFamily = f)
            Spacer(Modifier.height(12.dp))
            listOf("QEMU Gunyah" to onSelectQvm, "crosvm" to onSelectCvm).forEach { (s, onClick) ->
                TypeItem(s, onClick)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TypeItem(label: String, onClick: () -> Unit) = Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = Color.White,
    modifier = Modifier.fillMaxWidth()
) {
    Text(
        label,
        Modifier.padding(16.dp),
        fontSize = 14.sp,
        fontFamily = localFont.current,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFE95420)
    )
}