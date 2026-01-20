package com.haramshield.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.items
import androidx.hilt.navigation.compose.hiltViewModel
import com.haramshield.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPinSetup: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Detection Categories Section
            Text(
                text = stringResource(R.string.detection_categories),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
            
            SwitchSettingsItem(
                title = stringResource(R.string.nsfw_content),
                checked = uiState.nsfwEnabled,
                onCheckedChange = { viewModel.setNsfwEnabled(it) }
            )
            
            SwitchSettingsItem(
                title = stringResource(R.string.alcohol),
                checked = uiState.alcoholEnabled,
                onCheckedChange = { viewModel.setAlcoholEnabled(it) }
            )
            
            SwitchSettingsItem(
                title = stringResource(R.string.tobacco),
                checked = uiState.tobaccoEnabled,
                onCheckedChange = { viewModel.setTobaccoEnabled(it) }
            )
            
            SwitchSettingsItem(
                title = stringResource(R.string.gambling),
                checked = uiState.gamblingEnabled,
                onCheckedChange = { viewModel.setGamblingEnabled(it) }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sensitivity Section
            Text(
                text = stringResource(R.string.sensitivity),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
            
            SliderSettingsItem(
                title = "Detection Sensitivity",
                value = uiState.sensitivity,
                onValueChange = { viewModel.setSensitivity(it) },
                valueRange = 0.3f..0.95f
            )
            
            // Lockout Duration
            Text(
                text = stringResource(R.string.lockout_duration),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
            
            SliderSettingsItem(
                title = stringResource(R.string.minutes_format, uiState.lockoutDurationMinutes),
                value = uiState.lockoutDurationMinutes.toFloat(),
                onValueChange = { viewModel.setLockoutDuration(it.toInt()) },
                valueRange = 5f..60f,
                steps = 10
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Other Settings
            ClickableSettingsItem(
                title = stringResource(R.string.whitelist),
                icon = Icons.Default.Apps,
                onClick = onNavigateToWhitelist
            )
            
            ClickableSettingsItem(
                title = stringResource(R.string.pin_settings),
                icon = Icons.Default.Lock,
                onClick = onNavigateToPinSetup
            )
            
            ClickableSettingsItem(
                title = stringResource(R.string.about),
                icon = Icons.Default.Info,
                onClick = onNavigateToAbout
            )
            
            // Blocked Words Management
            var showBlockedWordsDialog by remember { mutableStateOf(false) }
            
            ClickableSettingsItem(
                title = "Blocked Words",
                icon = Icons.Default.Block,
                onClick = { showBlockedWordsDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // DANGER ZONE
            Text(
                text = "DANGER ZONE",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            
            // ... (Deactivate Button Code) ...
            
            if (showBlockedWordsDialog) {
                BlockedWordsDialog(
                    words = uiState.customBlockedWords,
                    onAddWord = { viewModel.addBlockedWord(it) },
                    onRemoveWord = { viewModel.removeBlockedWord(it) },
                    onDismiss = { showBlockedWordsDialog = false }
                )
            }
            
            var showDeactivateDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showDeactivateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deactivate Shield", color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (showDeactivateDialog) {
                DeactivateConfirmationDialog(
                    onConfirm = {
                        viewModel.disableMonitoring()
                        showDeactivateDialog = false
                    },
                    onDismiss = { showDeactivateDialog = false },
                    initialTimerSeconds = (com.haramshield.Constants.DEACTIVATE_DELAY_MS / 1000).toInt() // Anti-Regret Timer
                )
            }
        }
    }
}

@Composable
fun DeactivateConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    initialTimerSeconds: Int
) {
    var timeLeft by remember { mutableIntStateOf(initialTimerSeconds) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Deactivate Shield?", 
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            ) 
        },
        text = {
            Column {
                Text("Warning: You are about to disable your protection. The shield will go down.")
                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Timer
                Text(
                    text = "Anti-Regret Timer: ${timeLeft}s",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally),
                    color = if (timeLeft > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Wait for the timer to confirm deletion of your defense.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = timeLeft == 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Confirm Deactivate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SwitchSettingsItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun SliderSettingsItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun ClickableSettingsItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors()
    )
}

@Composable
fun BlockedWordsDialog(
    words: Set<String>,
    onAddWord: (String) -> Unit,
    onRemoveWord: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newWord by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Blocked Words") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                // Input Field
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    label = { Text("Add Word") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (newWord.isNotBlank()) {
                                onAddWord(newWord)
                                newWord = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // List of Words
                if (words.isEmpty()) {
                    Text(
                        "No custom words added.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(words.toList().sorted()) { word ->
                            ListItem(
                                headlineContent = { Text(word) },
                                trailingContent = {
                                    IconButton(onClick = { onRemoveWord(word) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                                    }
                                },
                                modifier = Modifier.height(48.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
