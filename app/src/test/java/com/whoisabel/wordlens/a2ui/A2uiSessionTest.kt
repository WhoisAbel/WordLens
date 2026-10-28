package com.whoisabel.wordlens.a2ui

import com.whoisabel.wordlens.scan.Blocks
import com.whoisabel.wordlens.a2ui.agent.COPY_EVENT
import com.whoisabel.wordlens.a2ui.agent.buildA2uiMessages
import com.whoisabel.wordlens.a2ui.agent.copyText
import com.whoisabel.wordlens.a2ui.engine.A2uiSession
import com.whoisabel.wordlens.a2ui.engine.ComponentRegistry

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiDataPath
import com.whoisabel.wordlens.a2ui.agent.component
import com.whoisabel.wordlens.a2ui.agent.createSurface
import com.whoisabel.wordlens.a2ui.agent.obj
import com.whoisabel.wordlens.a2ui.agent.str
import com.whoisabel.wordlens.a2ui.agent.updateComponents
import com.whoisabel.wordlens.a2ui.engine.WordLensCatalog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class A2uiSessionTest {
    @Test
    fun engineAcceptsGeneratedMessages() = runBlocking(Dispatchers.Default) {
        val session = A2uiSession {}
        val job = launch { session.run() }
        val messages = buildA2uiMessages("s1", listOf(listOf("hello", "world")))
        messages.forEach(session::send)

        val surface = withTimeout(5_000) {
            var s: A2uiCoreSurfaceModel?
            do {
                delay(20)
                s = session.processor.activeSurfaces.value.filterIsInstance<A2uiCoreSurfaceModel>().firstOrNull()
            } while (s == null || s.dataModel[A2uiDataPath("/lines/1")] == null)
            s
        }
        assertEquals("world", surface.dataModel[A2uiDataPath("/lines/1")])
        // The engine validates each component against the catalog and drops invalid ones,
        // so every generated component reaching the registry proves the whole layout is valid.
        val generated = Json.parseToJsonElement(messages[1]).jsonObject["updateComponents"]!!.jsonObject["components"]!!
            .jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet()
        assertEquals(generated, (surface.componentRegistry as ComponentRegistry).components.keys)
        job.cancel()
    }

    @Test
    fun editedValuesFlowIntoCopyEvent() = runBlocking(Dispatchers.Default) {
        val session = A2uiSession {}
        val job = launch { session.run() }
        val event = async { session.processor.outboundEvents.first() as A2uiClientEventMessage }
        delay(100) // let the collector subscribe; outboundEvents has no replay
        buildA2uiMessages("s2", listOf(listOf("hello", "world"))).forEach(session::send)

        val surface = withTimeout(5_000) {
            var s: A2uiCoreSurfaceModel?
            do {
                delay(20)
                s = session.processor.activeSurfaces.value.filterIsInstance<A2uiCoreSurfaceModel>().firstOrNull()
            } while (s == null || s.dataModel[A2uiDataPath("/lines/1")] == null)
            s
        }
        surface.dataModel.update(A2uiDataPath("/lines/1"), "edited") // what a TextField edit does
        val action = (surface.componentRegistry as ComponentRegistry).components["copy"]!!.properties["action"]
        @Suppress("UNCHECKED_CAST")
        surface.dispatchAction("copy", action as Map<String, Any?>)

        val received = withTimeout(5_000) { event.await() }
        assertEquals(COPY_EVENT, received.type)
        assertEquals("hello\nedited", copyText(received.context))
        job.cancel()
    }

    @Test
    fun functionCallActionRunsOpenUrlOnTheClient() = runBlocking(Dispatchers.Default) {
        val opened = CompletableDeferred<String>()
        val session = A2uiSession { url -> opened.complete(url) }
        val job = launch { session.run() }
        val action = obj("functionCall" to obj("call" to str("openUrl"), "args" to obj("url" to str("tel:+491512345"))))
        session.send(createSurface("s3", WordLensCatalog.ID))
        session.send(
            updateComponents(
                "s3",
                listOf(
                    component("root", "Button", "variant" to str("default"), "child" to str("label"), "action" to action),
                    component("label", "Text", "text" to str("Call")),
                ),
            )
        )
        val button = withTimeout(5_000) {
            var payload: Map<String, Any?>? = null
            var surface: A2uiCoreSurfaceModel? = null
            while (payload == null) {
                delay(20)
                surface = session.processor.activeSurfaces.value.filterIsInstance<A2uiCoreSurfaceModel>().firstOrNull()
                payload = (surface?.componentRegistry as? ComponentRegistry)?.components?.get("root")?.properties
            }
            surface!! to payload
        }
        @Suppress("UNCHECKED_CAST")
        button.first.dispatchAction("root", button.second["action"] as Map<String, Any?>)

        assertEquals("tel:+491512345", withTimeout(5_000) { opened.await() })
        job.cancel()
    }
}
