package com.whoisabel.wordlens

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            (component.properties["children"] as? List<*>)?.forEach { Render(surface, it as String) }
        }
        "Card" -> Card(modifier.fillMaxWidth()) {
            Render(surface, component.properties["child"] as String, Modifier.padding(12.dp))
        }
        "Text" -> Text(
            resolve("text"),
            style = if (component.properties["variant"] == "title") MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            modifier = modifier,
        )
        "Button" -> Button(
            onClick = {
                @Suppress("UNCHECKED_CAST")
                surface.dispatchAction(id, component.properties["action"] as Map<String, Any?>)
            },
            modifier = modifier.fillMaxWidth(),
        ) { Render(surface, component.properties["child"] as String) }
        "TextField" -> {
            val path = (component.properties["value"] as? Map<*, *>)?.get("path") as? String
            OutlinedTextField(
                value = resolve("value"),
                onValueChange = { if (path != null) surface.dataModel.update(A2uiDataPath(path), it) },
                label = { Text(resolve("label")) },
                modifier = modifier.fillMaxWidth(),
            )
        }
    }
}
