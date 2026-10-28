package com.whoisabel.wordlens.a2ui.engine

import androidx.a2ui.engine.processor.A2uiCoreMessageProcessor
import androidx.a2ui.model.catalog.functions.A2uiUrlOpener
import androidx.a2ui.model.processor.A2uiJsonMessageParser
import androidx.a2ui.model.processor.processInput

/**
 * One client-side A2UI session: the official parser and message processor, wired to our catalog,
 * data model and registry. Feed it JSON with [send]; read UI state from `processor.activeSurfaces`.
 *
 * [urlOpener] runs when a component's action calls the `openUrl` function (e.g. "tel:…", "mailto:…").
 */
class A2uiSession(urlOpener: A2uiUrlOpener) {
    val processor = A2uiCoreMessageProcessor(listOf(WordLensCatalog(urlOpener)), ::MapDataModel, ::ComponentRegistry)
    private val parser = A2uiJsonMessageParser { JsonElementReader(it) }

    /** Must run for the processor to consume messages; launch it in a coroutine scope. */
    suspend fun run() = processor.collectMessages()

    /** Parses one A2UI JSON message and queues it; parse errors are reported via `processor.outboundEvents`. */
    fun send(json: String) = processor.processInput(parser, json)
}
