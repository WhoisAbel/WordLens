package com.whoisabel.wordlens

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.whoisabel.wordlens.ui.theme.WordLensTheme
import kotlinx.coroutines.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordLensTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScannerScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Error(val message: String) : ScanState
    data object Done : ScanState
}

@Composable
private fun ScannerScreen(modifier: Modifier = Modifier) {
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    val context = LocalContext.current
    val a2ui = remember { A2uiSession() }
    LaunchedEffect(a2ui) { a2ui.run() }
    LaunchedEffect(a2ui) {
        a2ui.processor.outboundEvents.collect { event ->
            if (event is A2uiClientEventMessage && event.type == COPY_EVENT) {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("WordLens", copyText(event.context)))
            }
        }
    }
    val surface = a2ui.processor.activeSurfaces.collectAsState().value.filterIsInstance<A2uiCoreSurfaceModel>().firstOrNull()
    LaunchedEffect(imageUri) {
        val uri = imageUri ?: return@LaunchedEffect
        state = ScanState.Loading
        state = try {
            val blocks = scanText(context, uri)
            if (blocks.isEmpty()) ScanState.Error("No text found in this image.") else {
                surface?.let { a2ui.send(deleteSurface(it.id)) }
                buildA2uiMessages("scan-${System.nanoTime()}", blocks).forEach(a2ui::send)
                ScanState.Done
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ScanState.Error("Text recognition failed: ${e.message}")
        }
    }
    val bitmap = remember(imageUri) {
        imageUri?.let { ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) }
    }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(
            onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Select image") }
        bitmap?.let {
            Image(it.asImageBitmap(), contentDescription = "Selected image", contentScale = ContentScale.Fit)
        }
        when (val s = state) {
            ScanState.Idle -> {}
            ScanState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            is ScanState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            ScanState.Done -> surface?.let { A2uiSurface(it) }
        }
    }
}
