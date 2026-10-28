package com.whoisabel.wordlens.ui.sections

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.whoisabel.wordlens.ui.theme.Violet

/** The picked image, with an animated scan line on top while ML Kit runs. */
@Composable
fun ImagePreview(uri: Uri, scanning: Boolean) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        // Downscale big camera photos; drawing a 50 MP bitmap would crash the canvas.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = maxOf(1, maxOf(info.size.width, info.size.height) / 1600)
            decoder.setTargetSize(info.size.width / scale, info.size.height / scale)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap.asImageBitmap(),
            contentDescription = "Selected image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
        )
        if (scanning) ScanOverlay(Modifier.matchParentSize())
    }
}

@Composable
fun ScanningIndicator() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text("Reading text on-device…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Dims the image and sweeps a glowing line over it. */
@Composable
private fun ScanOverlay(modifier: Modifier) {
    val sweep by rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    Canvas(modifier) {
        drawRect(Color.Black.copy(alpha = 0.35f))
        val y = size.height * sweep
        val trail = 140f
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Violet.copy(alpha = 0.6f)), startY = y - trail, endY = y),
            topLeft = Offset(0f, y - trail),
            size = Size(size.width, trail),
        )
        drawLine(Color.White, Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx())
    }
}
