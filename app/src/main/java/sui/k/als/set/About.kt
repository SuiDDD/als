package sui.k.als.set

import android.content.res.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import sui.k.als.*

@Composable
fun About(onBack: () -> Unit) {
    BackHandler { onBack() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .padding(9.dp)
                .then(
                    if (isLandscape) Modifier.fillMaxWidth(1f / 3f).fillMaxHeight()
                    else Modifier.fillMaxWidth().height(configuration.screenHeightDp.dp / 3)
                )
                .clip(RoundedCornerShape(9.dp))
                .drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            0.0f to Color(0xFF111111),
                            1.0f to Color(0xFF444444),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f)
                        )
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "A L S",
                color = Color.White,
                fontSize = 36.sp,
                fontFamily = localFont.current
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "26.4.25 P3",
                color = Color.Gray,
                fontSize = 9.sp,
                fontFamily = localFont.current
            )
        }
    }
}