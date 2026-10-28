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
            } while (s == null || s.dataModel[A2uiDataPath("/text")] == null)
            s
        }
        assertEquals("hello\nworld", surface.dataModel[A2uiDataPath("/text")])
        assertEquals(setOf("root", "text"), (surface.componentRegistry as ComponentRegistry).components.keys)
        job.cancel()
    }
}
