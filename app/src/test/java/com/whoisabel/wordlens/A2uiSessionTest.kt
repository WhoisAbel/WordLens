package com.whoisabel.wordlens

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiDataPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class A2uiSessionTest {
    @Test
    fun engineAcceptsGeneratedMessages() = runBlocking(Dispatchers.Default) {
        val session = A2uiSession()
        val job = launch { session.run() }
        buildA2uiMessages("s1", listOf(listOf("hello", "world"))).forEach(session::send)

        val surface = withTimeout(5_000) {
            var s: A2uiCoreSurfaceModel?
            do {
                delay(20)
                s = session.processor.activeSurfaces.value.filterIsInstance<A2uiCoreSurfaceModel>().firstOrNull()
            } while (s == null || s.dataModel[A2uiDataPath("/lines/1")] == null)
            s
        }
        assertEquals("world", surface.dataModel[A2uiDataPath("/lines/1")])
        assertEquals(
            setOf("root", "summary", "card0", "col0", "field0", "field1"),
            (surface.componentRegistry as ComponentRegistry).components.keys,
        )
        job.cancel()
    }
}
