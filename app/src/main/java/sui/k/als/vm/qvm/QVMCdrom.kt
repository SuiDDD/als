package sui.k.als.vm.qvm
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sui.k.als.R
import sui.k.als.localFont
import sui.k.als.ui.ALSList
@Composable
fun QVMCdrom(state: MutableMap<String, Any>) {
    val path = state["path"]?.toString() ?: ""
    val index = state["index"]?.toString() ?: ""
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("CD-ROM", color = Color.Gray, fontSize = 10.sp, fontFamily = localFont.current)
        }
        ALSList(stringResource(R.string.cdrom_path), value = path, first = true, backgrounds = if (path.isEmpty()) Color.Red else null, onValueChange = { state["path"] = it })
        ALSList(stringResource(R.string.boot_index), value = index, last = true, onValueChange = { state["index"] = it })
    }
}
