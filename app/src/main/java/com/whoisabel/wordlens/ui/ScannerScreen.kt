package com.whoisabel.wordlens.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whoisabel.wordlens.R
import com.whoisabel.wordlens.ui.sections.EmptyState
import com.whoisabel.wordlens.ui.sections.ErrorCard
import com.whoisabel.wordlens.ui.sections.Hero
import com.whoisabel.wordlens.ui.sections.ImagePreview
import com.whoisabel.wordlens.ui.sections.ResultSection
import com.whoisabel.wordlens.ui.sections.ScanningIndicator
import com.whoisabel.wordlens.ui.theme.BrandGradient
import kotlinx.coroutines.launch

/** The only screen: hero, then either onboarding, a scan in progress, an error or the generated UI. */
@Composable
fun ScannerScreen(viewModel: ScannerViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val surface by viewModel.surface.collectAsState()

    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let(viewModel::scan) }
    val pickImage = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }

    val snackbar = remember { SnackbarHostState() }
    CopyToClipboardEffect(viewModel, snackbar)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
        floatingActionButton = { NewScanButton(visible = state != ScanState.Idle, onClick = pickImage) },
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
                    imageUri?.let { ImagePreview(it, scanning = state == ScanState.Loading) }
                    AnimatedContent(
                        state,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(150)) },
                        label = "scanState",
                    ) { s ->
                        when (s) {
                            ScanState.Idle -> Unit
                            ScanState.Loading -> ScanningIndicator()
                            is ScanState.Error -> ErrorCard(s.message)
                            ScanState.Done -> surface?.let { key(it.id) { ResultSection(it) } }
                        }
                    }
                }
            }
            StatusBarScrim(scroll)
        }
    }
}

/** Puts the text from each A2UI copy event on the clipboard, with a haptic tick and a snackbar. */
@Composable
private fun CopyToClipboardEffect(viewModel: ScannerViewModel, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.copiedText.collect { text ->
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("WordLens", text))
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar("Copied ${text.lines().size} lines to the clipboard") }
        }
    }
}

@Composable
private fun NewScanButton(visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(visible, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = { Icon(painterResource(R.drawable.ic_photo), contentDescription = null) },
            text = { Text("New scan") },
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

/** Fades in behind the status bar as the hero scrolls away, keeping the light icons readable. */
@Composable
private fun StatusBarScrim(scroll: ScrollState) {
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .graphicsLayer { alpha = (scroll.value / 300f).coerceIn(0f, 1f) }
            .background(BrandGradient),
    )
}
