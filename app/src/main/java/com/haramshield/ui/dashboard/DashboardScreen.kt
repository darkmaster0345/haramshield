package com.haramshield.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haramshield.R
import com.haramshield.ui.components.StatCard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // UI Theme Overrides for Cyber Look
    val cyberBlack = com.haramshield.ui.theme.ShieldCyberBlack
    val neonGreen = com.haramshield.ui.theme.ShieldNeonGreen
    val cyberGray = com.haramshield.ui.theme.ShieldCyberGray
    val neonGreenDim = com.haramshield.ui.theme.ShieldNeonGreenDim
    val textDim = com.haramshield.ui.theme.ShieldTextDim

    Scaffold(
        containerColor = cyberBlack,
        bottomBar = {
            // Simplified Bottom Nav Visual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cyberBlack)
                    .padding(vertical = 12.dp, horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(icon = Icons.Default.Shield, label = "Status", selected = true)
                BottomNavItem(icon = Icons.Default.History, label = "Block History", selected = false, onClick = onNavigateToHistory)
                BottomNavItem(icon = Icons.Default.Security, label = "My Account", selected = false, onClick = onNavigateToWhitelist)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. TOP HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = neonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HARAMSHIELD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Settings Button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = textDim,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Status Badge
                    Surface(
                        color = cyberGray,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, neonGreenDim)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isProtectionActive) neonGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isProtectionActive) "SHIELD ACTIVE" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isProtectionActive) neonGreen else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(label = "Violations Blocked", value = uiState.violationsBlocked)
                StatCard(label = "Apps Monitored", value = uiState.appsMonitored)
                StatCard(label = "Days Protected", value = uiState.daysProtected)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 2. MAIN OCTAGONAL SHIELD INDICATOR
            Box(
                modifier = Modifier.size(260.dp), // Slightly larger for glow
                contentAlignment = Alignment.Center
            ) {
                // "NOOR" Light Effect (Radial Gradient)
                // Pulse Animation tied to Scan
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.0f,
                    targetValue = 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing), // Matching High Power Interval
                        repeatMode = RepeatMode.Reverse
                    ), label = "pulse_alpha"
                )

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    neonGreen.copy(alpha = pulseAlpha), // Dynamic Pulse
                                    cyberBlack.copy(alpha = 0.0f)
                                ),
                                center = androidx.compose.ui.geometry.Offset.Unspecified,
                                radius = 300f
                            )
                        )
                )

                // Outer Octagon Border (Rotating)
                InfiniteRotatingOctagon(neonGreen, pulseAlpha)

                // Inner Content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = neonGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "VALIDATING",
                        style = MaterialTheme.typography.labelLarge,
                        color = textDim,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 3. LIVE NEURAL FEED (Scrolling)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = neonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE NEURAL FEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                StreamingIndicator()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Log Container (Scrolling)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                color = cyberGray,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, cyberGray)
            ) {
                val scrollState = rememberScrollState()
                LaunchedEffect(uiState.neuralFeedLogs) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(scrollState)
                ) {
                    uiState.neuralFeedLogs.forEach { log ->
                        var displayedText by remember { mutableStateOf("") }
                        LaunchedEffect(log) {
                            log.forEachIndexed { index, _ ->
                                displayedText = log.substring(0, index + 1)
                                delay(10)
                            }
                        }
                        Text(
                            text = displayedText,
                            style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = neonGreen.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. PAUSE PROTECTION BOX (with Snooze Duration Dialog)
            val isSnoozed = uiState.isSnoozed
            var showSnoozeDialog by remember { mutableStateOf(false) }
            
            // Calculate remaining time
            val snoozeRemainingText = if (isSnoozed && uiState.snoozeUntil > 0) {
                val remaining = (uiState.snoozeUntil - System.currentTimeMillis()) / 1000
                if (remaining > 0) {
                    val mins = remaining / 60
                    val secs = remaining % 60
                    "Resumes in ${mins}m ${secs}s"
                } else "Active"
            } else "Temporary bypass"
            
            Button(
                onClick = { if (!isSnoozed) showSnoozeDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isSnoozed) neonGreen.copy(alpha = 0.2f) else cyberGray),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSnoozed) neonGreen else cyberGray.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(cyberBlack.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSnoozed) Icons.Default.Timer else Icons.Default.History,
                            contentDescription = null,
                            tint = if (isSnoozed) neonGreen else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isSnoozed) "Protection Paused" else "Pause Protection",
                            color = if (isSnoozed) neonGreen else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = snoozeRemainingText,
                            color = textDim,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Snooze Duration Dialog
            if (showSnoozeDialog) {
                SnoozeDurationDialog(
                    onDismiss = { showSnoozeDialog = false },
                    onDurationSelected = { minutes ->
                        viewModel.snoozeProtection(minutes)
                        showSnoozeDialog = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 5. PROTECTION CATEGORIES (Tied to Lockouts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROTECTION CATEGORIES",
                    style = MaterialTheme.typography.labelMedium,
                    color = textDim,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "● ALL ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = neonGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // NSFW / Modesty (10m)
            CategoryCyberItem(
                title = "NSFW / Modesty",
                subtitle = "Neural detection active",
                duration = uiState.nsfwLockoutTime,
                icon = Icons.Default.Shield,
                isActive = uiState.isProtectionActive,
                onDurationSelected = { viewModel.setNsfwLockoutTime(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Alcohol / Tobacco (1m)
            CategoryCyberItem(
                title = "Alcohol / Tobacco",
                subtitle = "Object recognition active",
                duration = uiState.healthLockoutTime,
                icon = Icons.Default.History,
                isActive = uiState.isProtectionActive,
                onDurationSelected = { viewModel.setHealthLockoutTime(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit = {}
) {
    val neonGreen = com.haramshield.ui.theme.ShieldNeonGreen
    val textDim = com.haramshield.ui.theme.ShieldTextDim

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) neonGreen else textDim,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (selected) neonGreen else textDim,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun StreamingIndicator() {
    val neonGreen = com.haramshield.ui.theme.ShieldNeonGreen
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(neonGreen.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "STREAMING",
            style = MaterialTheme.typography.labelSmall,
            color = neonGreen.copy(alpha = alpha),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryCyberItem(
    title: String,
    subtitle: String,
    duration: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onDurationSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val lockoutTimes = listOf("1m", "5m", "10m", "15m", "30m", "1h")

    Surface(
        color = com.haramshield.ui.theme.ShieldCyberGray,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.haramshield.ui.theme.ShieldCyberLightGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(com.haramshield.ui.theme.ShieldCyberLightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isActive) "● Active" else "○ Inactive",
                        fontSize = 10.sp,
                        color = if (isActive) com.haramshield.ui.theme.ShieldNeonGreen else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = com.haramshield.ui.theme.ShieldTextDim)
            }

            // Duration indicator (Tied to dynamic lockouts)
            Box {
                Surface(
                    color = com.haramshield.ui.theme.ShieldCyberLightGray,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(
                        text = duration,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    lockoutTimes.forEach { time ->
                        DropdownMenuItem(
                            text = { Text(time) },
                            onClick = {
                                onDurationSelected(time)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfiniteRotatingOctagon(color: Color, pulseAlpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        val strokeWidth = 2.dp.toPx()
        val radius = size.minDimension / 2
        val center = center

        // Octagon path
        val path = androidx.compose.ui.graphics.Path()
        val sides = 8
        for (i in 0 until sides) {
            val angle = Math.toRadians((360.0 / sides * i) + rotation.toDouble())
            val x = (center.x + radius * Math.cos(angle)).toFloat()
            val y = (center.y + radius * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = color,
            alpha = 0.6f * (pulseAlpha + 0.4f), // Dynamic opacity
            style = Stroke(width = strokeWidth)
        )

        // Inner Octagon (Different direction)
        val innerPath = androidx.compose.ui.graphics.Path()
        val innerRadius = radius * 0.8f
        for (i in 0 until sides) {
            val angle = Math.toRadians((360.0 / sides * i) - (rotation * 0.5).toDouble())
            val x = (center.x + innerRadius * Math.cos(angle)).toFloat()
            val y = (center.y + innerRadius * Math.sin(angle)).toFloat()
            if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
        }
        innerPath.close()

        drawPath(
            path = innerPath,
            color = color,
            alpha = 0.3f,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun SnoozeDurationDialog(
    onDismiss: () -> Unit,
    onDurationSelected: (Int) -> Unit
) {
    val neonGreen = com.haramshield.ui.theme.ShieldNeonGreen
    val cyberBlack = com.haramshield.ui.theme.ShieldCyberBlack
    val cyberGray = com.haramshield.ui.theme.ShieldCyberGray
    val textDim = com.haramshield.ui.theme.ShieldTextDim
    
    val durations = listOf(
        5 to "5 minutes",
        15 to "15 minutes",
        30 to "30 minutes",
        60 to "1 hour"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cyberGray,
        title = {
            Text(
                text = "Pause Protection",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Select how long to pause protection:",
                    color = textDim,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                durations.forEach { (minutes, label) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onDurationSelected(minutes) },
                        color = cyberBlack.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, neonGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = neonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textDim)
            }
        }
    )
}




