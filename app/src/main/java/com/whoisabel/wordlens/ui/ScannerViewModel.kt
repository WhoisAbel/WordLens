package com.whoisabel.wordlens.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whoisabel.wordlens.a2ui.agent.COPY_EVENT
import com.whoisabel.wordlens.a2ui.agent.buildA2uiMessages
import com.whoisabel.wordlens.a2ui.agent.copyText
import com.whoisabel.wordlens.a2ui.agent.deleteSurface
import com.whoisabel.wordlens.a2ui.engine.A2uiSession
import com.whoisabel.wordlens.scan.scanText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ML Kit is often faster than this; waiting keeps the scan animation from flashing by. */
private const val MIN_SCAN_MS = 1200L

sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Error(val message: String) : ScanState
    data object Done : ScanState
}

/**
 * Runs the whole pipeline: image → ML Kit → A2UI messages → engine.
 * Lives across rotation, so the scan result and the A2UI surface survive configuration changes.
 */
class ScannerViewModel(app: Application) : AndroidViewModel(app) {
    // openUrl (tel:, mailto:, https:) is handed to whichever app can handle it: dialer, mail, browser.
    private val session = A2uiSession { url ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("WordLens", "No app can open $url", e)
        }
    }
    private var scanJob: Job? = null

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /** The surface the engine built from our messages, or null before the first scan. */
    val surface: StateFlow<A2uiCoreSurfaceModel?> = session.processor.activeSurfaces
        .map { it.filterIsInstance<A2uiCoreSurfaceModel>().firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Text to copy, emitted each time the generated "Copy all" button fires its A2UI event. */
    val copiedText: Flow<String> = session.processor.outboundEvents
        .filterIsInstance<A2uiClientEventMessage>()
        .filter { it.type == COPY_EVENT }
        .map { copyText(it.context) }

    init {
        viewModelScope.launch { session.run() }
    }

    fun scan(uri: Uri) {
        _imageUri.value = uri
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.value = ScanState.Loading
            _state.value = try {
                val start = SystemClock.uptimeMillis()
                val blocks = scanText(getApplication(), uri)
                delay(MIN_SCAN_MS - (SystemClock.uptimeMillis() - start))
                if (blocks.isEmpty()) {
                    ScanState.Error("No text found. Try a sharper photo with clear lettering.")
                } else {
                    surface.value?.let { session.send(deleteSurface(it.id)) }
                    buildA2uiMessages("scan-${System.nanoTime()}", blocks).forEach(session::send)
                    ScanState.Done
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                ScanState.Error("Text recognition failed: ${e.message}")
            }
        }
    }
}
