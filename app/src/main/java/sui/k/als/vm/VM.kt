package sui.k.als.vm
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sui.k.als.R
import sui.k.als.localAppFont
@Composable
fun VM() {
    val font = localAppFont.current
    var showCreateScreen by remember { mutableStateOf(false) }
    val accentColor = Color(0xFFE95420)
    if (showCreateScreen) {
        VMCreate(onBack = { showCreateScreen = false })
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = { showCreateScreen = true },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(9.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_add_vm),
                    fontFamily = font,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}