package com.whoisabel.wordlens.a2ui.render

import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.whoisabel.wordlens.a2ui.model.A2uiComponent
import com.whoisabel.wordlens.ui.theme.BrandGradient

/** Maps the spec's text variants (h1…h5, caption, body) to Material type styles. Headings use the brand gradient. */
@Composable
internal fun TextRenderer(surface: A2uiCoreSurfaceModel, text: A2uiComponent.Text, modifier: Modifier) {
    val type = MaterialTheme.typography
    val style = when (text.variant) {
        "h1" -> type.headlineLarge.heading()
        "h2" -> type.headlineMedium.heading()
        "h3" -> type.headlineSmall.heading()
        "h4" -> type.titleLarge.copy(fontWeight = FontWeight.Bold)
        "h5" -> type.titleMedium.copy(fontWeight = FontWeight.Bold)
        "caption" -> type.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> type.bodyLarge
    }
    Text(surface.resolveString(text.id, text.text), style = style, modifier = modifier)
}

private fun TextStyle.heading() = copy(brush = BrandGradient, fontWeight = FontWeight.Bold)

/** Tinted with the current content color, so it turns white inside a gradient button. */
@Composable
internal fun IconRenderer(icon: A2uiComponent.Icon, modifier: Modifier) {
    Icon(iconFor(icon.name), contentDescription = icon.name, modifier = modifier)
}

// ponytail: covers the spec names that exist in material-icons-core; others fall back to Info.
private fun iconFor(name: String): ImageVector = when (name) {
    "accountCircle" -> Icons.Default.AccountCircle
    "add" -> Icons.Default.Add
    "arrowBack" -> Icons.AutoMirrored.Filled.ArrowBack
    "arrowForward" -> Icons.AutoMirrored.Filled.ArrowForward
    "calendarToday", "event" -> Icons.Default.DateRange
    "call" -> Icons.Default.Call
    "check" -> Icons.Default.Check
    "close" -> Icons.Default.Close
    "delete" -> Icons.Default.Delete
    "edit" -> Icons.Default.Edit
    "favorite" -> Icons.Default.Favorite
    "home" -> Icons.Default.Home
    "locationOn" -> Icons.Default.LocationOn
    "lock" -> Icons.Default.Lock
    "mail" -> Icons.Default.Email
    "menu" -> Icons.Default.Menu
    "moreVert" -> Icons.Default.MoreVert
    "notifications" -> Icons.Default.Notifications
    "person" -> Icons.Default.Person
    "phone" -> Icons.Default.Phone
    "play" -> Icons.Default.PlayArrow
    "refresh" -> Icons.Default.Refresh
    "search" -> Icons.Default.Search
    "send" -> Icons.AutoMirrored.Filled.Send
    "settings" -> Icons.Default.Settings
    "share" -> Icons.Default.Share
    "shoppingCart" -> Icons.Default.ShoppingCart
    "star" -> Icons.Default.Star
    "warning", "error" -> Icons.Default.Warning
    else -> Icons.Default.Info
}
