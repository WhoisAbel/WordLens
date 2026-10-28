package com.whoisabel.wordlens.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whoisabel.wordlens.R
import com.whoisabel.wordlens.ui.components.GradientButton
import com.whoisabel.wordlens.ui.theme.BrandGradient

/** Shown before the first scan: a call to action plus the three pipeline steps. */
@Composable
fun EmptyState(onPick: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_text), contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Text("Scan text from any photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Receipts, signs, book pages… WordLens reads the text and builds an editable UI from it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            GradientButton(onClick = onPick) {
                Icon(painterResource(R.drawable.ic_photo), contentDescription = null)
                Text("Choose image")
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Step(1, "Recognize", "ML Kit reads the text, offline")
        Step(2, "Generate", "The text becomes A2UI messages")
        Step(3, "Interact", "Edit any line, copy everything")
    }
}

@Composable
private fun Step(number: Int, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
            Text("$number", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
