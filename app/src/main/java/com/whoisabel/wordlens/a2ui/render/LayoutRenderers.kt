package com.whoisabel.wordlens.a2ui.render

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whoisabel.wordlens.a2ui.model.A2uiComponent
import com.whoisabel.wordlens.ui.theme.BrandGradient

/** Vertical stack; children fade and slide in one after another. */
@Composable
internal fun ColumnRenderer(surface: A2uiCoreSurfaceModel, column: A2uiComponent.Column, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        column.children.forEachIndexed { i, childId ->
            key(childId) {
                val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                val delay = i * 80
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(350, delayMillis = delay)) + slideInVertically(tween(350, delayMillis = delay)) { it / 3 },
                ) { RenderComponent(surface, childId) }
            }
        }
    }
}

/** Horizontal stack; `justify` and `align` map to Compose arrangement and alignment. */
@Composable
internal fun RowRenderer(surface: A2uiCoreSurfaceModel, row: A2uiComponent.Row, modifier: Modifier) {
    val arrangement = when (row.justify) {
        "center" -> Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        "end" -> Arrangement.spacedBy(10.dp, Alignment.End)
        "spaceBetween" -> Arrangement.SpaceBetween
        "spaceAround" -> Arrangement.SpaceAround
        "spaceEvenly" -> Arrangement.SpaceEvenly
        else -> Arrangement.spacedBy(10.dp)
    }
    val alignment = when (row.align) {
        "start" -> Alignment.Top
        "end" -> Alignment.Bottom
        else -> Alignment.CenterVertically
    }
    Row(modifier, horizontalArrangement = arrangement, verticalAlignment = alignment) {
        row.children.forEach { RenderComponent(surface, it) }
    }
}

/** Rounded card with a gradient accent stripe on the left edge. */
@Composable
internal fun CardRenderer(surface: A2uiCoreSurfaceModel, card: A2uiComponent.Card, modifier: Modifier) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        RenderComponent(
            surface,
            card.child,
            Modifier
                .drawBehind { drawRect(BrandGradient, size = Size(5.dp.toPx(), size.height)) }
                .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
internal fun DividerRenderer(divider: A2uiComponent.Divider, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    if (divider.vertical) VerticalDivider(modifier, color = color) else HorizontalDivider(modifier, color = color)
}

/** Tab strip plus the selected tab's child. Which tab is selected is local UI state, not data model state. */
@Composable
internal fun TabsRenderer(surface: A2uiCoreSurfaceModel, tabs: A2uiComponent.Tabs, modifier: Modifier) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrimaryTabRow(selectedTabIndex = selected, containerColor = Color.Transparent) {
            tabs.tabs.forEachIndexed { i, tab ->
                Tab(
                    selected = i == selected,
                    onClick = { selected = i },
                    text = { Text(surface.resolveString(tabs.id, tab.title), fontWeight = FontWeight.SemiBold) },
                )
            }
        }
        key(selected) { RenderComponent(surface, tabs.tabs[selected].child) }
    }
}
