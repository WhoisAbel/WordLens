package com.whoisabel.wordlens.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whoisabel.wordlens.ui.theme.BrandGradient

/** Gradient header with the app name; extends behind the status bar. */
@Composable
fun Hero() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(BrandGradient)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.10f), size.width * 0.45f, Offset(size.width * 0.95f, size.height * 0.1f))
                drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.3f, Offset(size.width * 0.1f, size.height * 1.05f))
            }
            .statusBarsPadding()
            .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "ML KIT × A2UI",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = Color.White,
                modifier = Modifier.background(Color.White.copy(alpha = 0.18f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text("WordLens", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                "Snap a photo, get editable text. Everything runs on your device.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}
