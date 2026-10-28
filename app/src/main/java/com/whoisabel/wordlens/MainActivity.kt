package com.whoisabel.wordlens

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whoisabel.wordlens.ui.theme.BrandGradient
import com.whoisabel.wordlens.ui.theme.Violet
import com.whoisabel.wordlens.ui.theme.WordLensTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** ML Kit is often faster than this; waiting keeps the scan animation from flashing by. */
private const val MIN_SCAN_MS = 1200L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light status bar icons, since the hero gradient sits behind them.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()))
        setContent {
            WordLensTheme {
                ScannerScreen()
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
private fun ScannerScreen() {
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }
    val pickImage = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val a2ui = remember { A2uiSession() }
    LaunchedEffect(a2ui) { a2ui.run() }
    LaunchedEffect(a2ui) {
        a2ui.processor.outboundEvents.collect { event ->
            if (event is A2uiClientEventMessage && event.type == COPY_EVENT) {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("WordLens", copyText(event.context)))
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar("Copied ${event.context.size} lines to the clipboard")
                }
            }
        }
    }
    val surface = a2ui.processor.activeSurfaces.collectAsState().value.filterIsInstance<A2uiCoreSurfaceModel>().firstOrNull()
    LaunchedEffect(imageUri) {
        val uri = imageUri ?: return@LaunchedEffect
        state = ScanState.Loading
        state = try {
            val start = SystemClock.uptimeMillis()
            val blocks = scanText(context, uri)
            delay(MIN_SCAN_MS - (SystemClock.uptimeMillis() - start))
            if (blocks.isEmpty()) ScanState.Error("No text found. Try a sharper photo with clear lettering.") else {
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
        imageUri?.let {
            // Downscale big camera photos; drawing a 50 MP bitmap would crash the canvas.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, info, _ ->
                val scale = maxOf(1, maxOf(info.size.width, info.size.height) / 1600)
                decoder.setTargetSize(info.size.width / scale, info.size.height / scale)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
        floatingActionButton = {
            AnimatedVisibility(state != ScanState.Idle, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                ExtendedFloatingActionButton(
                    onClick = pickImage,
                    icon = { Icon(painterResource(R.drawable.ic_photo), contentDescription = null) },
                    text = { Text("New scan") },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Hero()
            Column(
                Modifier.navigationBarsPadding().padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state == ScanState.Idle) EmptyState(onPick = pickImage)
                bitmap?.let { ImagePreview(it, scanning = state == ScanState.Loading) }
                AnimatedContent(
                    state,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(150)) },
                    label = "scanState",
                ) { s ->
                    when (s) {
                        ScanState.Idle -> {}
                        ScanState.Loading -> Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Reading text on-device…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is ScanState.Error -> ErrorCard(s.message)
                        ScanState.Done -> surface?.let { key(it.id) { ResultSection(it) } }
                    }
                }
            }
        }
        // Fades in behind the status bar as the hero scrolls away, keeping the light icons readable.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = (scroll.value / 300f).coerceIn(0f, 1f) }
                .background(BrandGradient),
        )
        }
    }
}

@Composable
private fun Hero() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(BrandGradient)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.10f), size.width * 0.45f, Offset(size.width * 0.95f, size.height * 0.1f))
                drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.3f, Offset(size.width * 0.1f, size.height * 1.05f))
            }
            .statusBarsPadding()
            .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "ML KIT × A2UI",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = Color.White,
                modifier = Modifier.background(Color.White.copy(alpha = 0.18f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text("WordLens", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                "Snap a photo, get editable text. Everything runs on your device.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_text), contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Text("Scan text from any photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Receipts, signs, book pages… WordLens reads the text and builds an editable UI from it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            GradientButton(onClick = onPick) {
                Icon(painterResource(R.drawable.ic_photo), contentDescription = null)
                Text("Choose image")
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Step(1, "Recognize", "ML Kit reads the text, offline")
        Step(2, "Generate", "The text becomes A2UI messages")
        Step(3, "Interact", "Edit any line, copy everything")
    }
}

@Composable
private fun Step(number: Int, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
            Text("$number", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImagePreview(bitmap: Bitmap, scanning: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap.asImageBitmap(),
            contentDescription = "Selected image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
        )
        if (scanning) ScanOverlay(Modifier.matchParentSize())
    }
}

/** Dims the image and sweeps a glowing line over it while ML Kit runs. */
@Composable
private fun ScanOverlay(modifier: Modifier) {
    val sweep by rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    Canvas(modifier) {
        drawRect(Color.Black.copy(alpha = 0.35f))
        val y = size.height * sweep
        val trail = 140f
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Violet.copy(alpha = 0.6f)), startY = y - trail, endY = y),
            topLeft = Offset(0f, y - trail),
            size = Size(size.width, trail),
        )
        drawLine(Color.White, Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx())
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", style = MaterialTheme.typography.titleLarge)
            Column {
                Text("Couldn't read this one", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ResultSection(surface: A2uiCoreSurfaceModel) {
    val componentCount = (surface.componentRegistry as ComponentRegistry).components.size
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Generated UI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "A2UI · $componentCount components",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        A2uiSurface(surface)
    }
}

/** Pill button filled with the brand gradient; used by the app and by the A2UI Button component. */
@Composable
fun GradientButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
        contentPadding = PaddingValues(),
    ) {
        Row(
            Modifier.fillMaxSize().background(BrandGradient),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium) { content() }
        }
    }
}
