package com.whoisabel.wordlens

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
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
        "Text" -> Text(resolve("text"), style = MaterialTheme.typography.bodyLarge, modifier = modifier)
    }
}
