package com.whoisabel.wordlens.scan

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Recognized text: one entry per ML Kit text block, each holding that block's lines. */
typealias Blocks = List<List<String>>

/** Runs on-device ML Kit OCR on [uri]. Returns an empty list when no text is found. */
suspend fun scanText(context: Context, uri: Uri): Blocks {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val result = recognizer.process(InputImage.fromFilePath(context, uri)).await()
        return result.textBlocks.map { block -> block.lines.map { it.text } }
    } finally {
        recognizer.close()
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
