package com.whoisabel.wordlens.a2ui.render

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.whoisabel.wordlens.a2ui.engine.ComponentRegistry
import com.whoisabel.wordlens.a2ui.model.A2uiComponent

/**
 * Draws an A2UI surface, starting from the component with id "root".
 *
 * The official engine parses, validates and binds; this package only turns each typed
 * component into a Material 3 widget. One renderer per component type, grouped by file:
 * [LayoutRenderers], [ContentRenderers], [InputRenderers].
 */
@Composable
fun A2uiSurface(surface: A2uiCoreSurfaceModel, modifier: Modifier = Modifier) {
    RenderComponent(surface, "root", modifier)
}

/** Looks up a component by id, converts it to its typed model and draws it. */
@Composable
internal fun RenderComponent(surface: A2uiCoreSurfaceModel, id: String, modifier: Modifier = Modifier) {
    val payload = (surface.componentRegistry as ComponentRegistry).components[id] ?: return
    val component = remember(payload) { A2uiComponent.from(payload) }

    // Exhaustive: adding a new A2uiComponent subtype fails compilation until it is rendered here.
    when (component) {
        is A2uiComponent.Column -> ColumnRenderer(surface, component, modifier)
        is A2uiComponent.Row -> RowRenderer(surface, component, modifier)
        is A2uiComponent.Card -> CardRenderer(surface, component, modifier)
        is A2uiComponent.Divider -> DividerRenderer(component, modifier)
        is A2uiComponent.Tabs -> TabsRenderer(surface, component, modifier)
        is A2uiComponent.Text -> TextRenderer(surface, component, modifier)
        is A2uiComponent.Icon -> IconRenderer(component, modifier)
        is A2uiComponent.TextField -> TextFieldRenderer(surface, component, modifier)
        is A2uiComponent.Button -> ButtonRenderer(surface, component, modifier)
        is A2uiComponent.Unknown -> Unit
    }
}

/**
 * Resolves a dynamic value through the engine: a literal is returned as is, a `{"path": …}`
 * binding is read from the data model. Called during composition, so data model changes recompose.
 */
internal fun A2uiCoreSurfaceModel.resolveString(componentId: String, raw: Any?): String =
    evaluatePayload(componentId, { dataModel[it] }, A2uiDataPath("/"), raw) as? String ?: ""
