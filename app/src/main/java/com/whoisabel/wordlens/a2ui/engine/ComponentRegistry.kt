package com.whoisabel.wordlens.a2ui.engine

import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.runtime.mutableStateMapOf

/** Receives validated components from the engine and keeps the latest one per id for Compose to read. */
class ComponentRegistry : A2uiCoreComponentRegistry {
    val components = mutableStateMapOf<String, A2uiComponentPayload>()

    override fun update(components: List<A2uiComponentPayload>) = components.forEach { this.components[it.id] = it }

    override fun reportError(id: String, exception: A2uiException) {}

    override fun close() = components.clear()
}
