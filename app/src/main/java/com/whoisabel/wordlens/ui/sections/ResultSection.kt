package com.whoisabel.wordlens.ui.sections

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whoisabel.wordlens.a2ui.engine.ComponentRegistry
import com.whoisabel.wordlens.a2ui.render.A2uiSurface

/** Header with a live component count, then the A2UI-rendered surface. */
@Composable
fun ResultSection(surface: A2uiCoreSurfaceModel) {
    val componentCount = (surface.componentRegistry as ComponentRegistry).components.size
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Generated UI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "A2UI · $componentCount components",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        A2uiSurface(surface)
    }
}
