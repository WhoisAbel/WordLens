package com.whoisabel.wordlens.a2ui.render

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whoisabel.wordlens.ui.components.GradientButton
import com.whoisabel.wordlens.a2ui.model.A2uiComponent

/** Two-way binding: shows the value at its data model path and writes edits back to that path. */
@Composable
internal fun TextFieldRenderer(surface: A2uiCoreSurfaceModel, field: A2uiComponent.TextField, modifier: Modifier) {
    OutlinedTextField(
        value = surface.resolveString(field.id, field.value),
        onValueChange = { new -> field.boundPath?.let { surface.dataModel.update(A2uiDataPath(it), new) } },
        label = { Text(surface.resolveString(field.id, field.label)) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Hands the button's action to the engine, which resolves its context and emits an outbound event. */
@Composable
internal fun ButtonRenderer(surface: A2uiCoreSurfaceModel, button: A2uiComponent.Button, modifier: Modifier) {
    GradientButton(
        onClick = { surface.dispatchAction(button.id, button.action) },
        modifier = modifier.padding(top = 4.dp),
    ) { RenderComponent(surface, button.child) }
}
