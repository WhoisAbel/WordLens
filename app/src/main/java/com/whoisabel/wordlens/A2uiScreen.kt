package com.whoisabel.wordlens

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whoisabel.wordlens.ui.theme.BrandGradient

/**
 * Thin Compose mapper over the official A2UI engine: the engine parses, validates and binds;
 * this file only turns each resolved component into a Material 3 widget.
 */
@Composable
fun A2uiSurface(surface: A2uiCoreSurfaceModel, modifier: Modifier = Modifier) {
    Render(surface, "root", modifier)
}

@Composable
private fun Render(surface: A2uiCoreSurfaceModel, id: String, modifier: Modifier = Modifier) {
    val component = (surface.componentRegistry as ComponentRegistry).components[id] ?: return
    // Resolves literals and {"path": ...} bindings against the surface's data model.
    fun resolve(key: String): String =
        surface.evaluatePayload(id, { surface.dataModel[it] }, A2uiDataPath("/"), component.properties[key]) as? String ?: ""

    when (component.type) {
        "Column" -> Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Children fade and slide in one after another.
            (component.properties["children"] as? List<*>)?.forEachIndexed { i, child ->
                key(child) {
                    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(
                        visible,
                        enter = fadeIn(tween(350, delayMillis = i * 80)) + slideInVertically(tween(350, delayMillis = i * 80)) { it / 3 },
                    ) { Render(surface, child as String) }
                }
            }
        }
        "Card" -> Card(
            modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Render(
                surface,
                component.properties["child"] as String,
                Modifier
                    .drawBehind { drawRect(BrandGradient, size = Size(5.dp.toPx(), size.height)) }
                    .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
        "Text" -> Text(
            resolve("text"),
            style = if (component.properties["variant"] == "title") {
                MaterialTheme.typography.headlineSmall.copy(brush = BrandGradient, fontWeight = FontWeight.Bold)
            } else {
                LocalTextStyle.current
            },
            modifier = modifier,
        )
        "Button" -> GradientButton(
            onClick = {
                @Suppress("UNCHECKED_CAST")
                surface.dispatchAction(id, component.properties["action"] as Map<String, Any?>)
            },
            modifier = modifier.padding(top = 4.dp),
        ) { Render(surface, component.properties["child"] as String) }
        "TextField" -> {
            val path = (component.properties["value"] as? Map<*, *>)?.get("path") as? String
            OutlinedTextField(
                value = resolve("value"),
                onValueChange = { if (path != null) surface.dataModel.update(A2uiDataPath(path), it) },
                label = { Text(resolve("label")) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = modifier.fillMaxWidth(),
            )
        }
    }
}
