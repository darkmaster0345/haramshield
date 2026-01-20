package com.haramshield.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * List item component for app selection (whitelist, etc.)
 */
@Composable
fun AppListItem(
    appName: String,
    packageName: String,
    icon: Drawable? = null,
    isSelected: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            if (icon != null) {
                val bitmap = icon.toBitmap(48, 48)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = appName,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // App info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Trailing content (checkbox, toggle, etc.)
            trailing?.invoke()
        }
    }
}

/**
 * Simplified app list item without drawable (for when icon isn't available)
 */
@Composable
fun AppListItemSimple(
    appName: String,
    packageName: String,
    isWhitelisted: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AppListItem(
        appName = appName,
        packageName = packageName,
        isSelected = isWhitelisted,
        trailing = {
            Checkbox(
                checked = isWhitelisted,
                onCheckedChange = onToggle
            )
        },
        onClick = { onToggle(!isWhitelisted) },
        modifier = modifier
    )
}
