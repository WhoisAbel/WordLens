package com.whoisabel.wordlens.a2ui.engine

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiActionSchema
import androidx.a2ui.model.schema.commontypes.A2uiChildListSchema
import androidx.a2ui.model.schema.commontypes.A2uiComponentIdSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema

/**
 * The catalog tells the engine which components exist and what properties each accepts.
 * Every incoming component is validated against these schemas before it reaches the UI.
 */
object WordLensCatalog : A2uiCoreCatalog {
    const val ID = "wordlens:catalog/v1"

    override val id = ID
    override val componentDefinitions = A2uiCoreComponentDefinitionCollection(
        listOf(
            Component("Column", "Vertical layout", mapOf("children" to A2uiChildListSchema.DEFAULT_INSTANCE), setOf("children")),
            Component("Card", "Elevated container", mapOf("child" to A2uiComponentIdSchema.DEFAULT_INSTANCE), setOf("child")),
            Component("Text", "Static or bound text", mapOf("text" to A2uiDynamicStringSchema.DEFAULT_INSTANCE, "variant" to A2uiStringSchema()), setOf("text")),
            Component("TextField", "Editable text bound to the data model", mapOf("label" to A2uiDynamicStringSchema.DEFAULT_INSTANCE, "value" to A2uiDynamicStringSchema.DEFAULT_INSTANCE), setOf("value")),
            Component("Button", "Fires an action", mapOf("child" to A2uiComponentIdSchema.DEFAULT_INSTANCE, "action" to A2uiActionSchema.DEFAULT_INSTANCE), setOf("child", "action")),
        )
    )
    override val functions = A2uiFunctionCollection()
}

private class Component(
    override val name: String,
    override val description: String,
    props: Map<String, A2uiSchema>,
    required: Set<String> = emptySet(),
) : A2uiCoreComponentDefinition {
    override val propertySchema: A2uiSchema = A2uiObjectSchema(props, required)
}
