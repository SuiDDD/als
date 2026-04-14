package sui.k.als.vm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import sui.k.als.localFont

@Composable
fun CVMCreate(onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(onClick = onExit),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "crosvm",
            fontSize = 20.sp,
            fontFamily = localFont.current,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}