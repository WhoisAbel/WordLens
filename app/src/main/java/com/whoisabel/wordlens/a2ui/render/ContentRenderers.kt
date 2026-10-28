package com.whoisabel.wordlens.a2ui.render

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.whoisabel.wordlens.a2ui.model.A2uiComponent
import com.whoisabel.wordlens.ui.theme.BrandGradient

@Composable
internal fun TextRenderer(surface: A2uiCoreSurfaceModel, text: A2uiComponent.Text, modifier: Modifier) {
    val style = when (text.variant) {
        "title" -> MaterialTheme.typography.headlineSmall.copy(brush = BrandGradient, fontWeight = FontWeight.Bold)
        else -> LocalTextStyle.current
    }
    Text(surface.resolveString(text.id, text.text), style = style, modifier = modifier)
}
