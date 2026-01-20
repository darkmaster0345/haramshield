package com.haramshield.ui.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.haramshield.R
import com.haramshield.util.PermissionHelper
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissionType: PermissionType? = null
)

enum class PermissionType {
    ACCESSIBILITY, OVERLAY, NOTIFICATION, DEVICE_ADMIN, USAGE_STATS
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Check permissions when app resumes (returning from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to HaramShield",
            description = "Protect your digital environment with secure, private, and intelligent content filtering.",
            icon = Icons.Default.Shield
        ),
        OnboardingPage(
            title = "How it Works",
            description = "HaramShield uses on-device AI to detect and block prohibited content in real-time. No data ever leaves your phone.",
            icon = Icons.Default.Psychology
        ),
        OnboardingPage(
            title = "Accessibility Service",
            description = "Required to detect when you open apps and to lock the screen when violations are found.",
            icon = Icons.Default.AccessibilityNew,
            permissionType = PermissionType.ACCESSIBILITY
        ),
        OnboardingPage(
            title = "Display Over Overlay",
            description = "Required to show the lockout screen immediately over other apps.",
            icon = Icons.Default.Layers,
            permissionType = PermissionType.OVERLAY
        ),
        OnboardingPage(
            title = "Notifications",
            description = "Required to keep the service running reliably in the background.",
            icon = Icons.Default.Notifications,
            permissionType = PermissionType.NOTIFICATION
        ),
        OnboardingPage(
            title = "App Usage Stats",
            description = "Required to identify which app you are currently using.",
            icon = Icons.Default.DataUsage,
            permissionType = PermissionType.USAGE_STATS
        ),
        OnboardingPage(
            title = "All Set!",
            description = "You're ready to go. You can configure detection sensitivity and categories in Settings.",
            icon = Icons.Default.CheckCircle
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Permission Launchers
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { viewModel.checkPermissions() }
    )
    
    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { viewModel.checkPermissions() }
    )
    
    fun requestPermission(type: PermissionType) {
        val helper = PermissionHelper(context)
        when (type) {
            PermissionType.ACCESSIBILITY -> {
                activityResultLauncher.launch(helper.getAccessibilitySettingsIntent())
            }
            PermissionType.OVERLAY -> {
                activityResultLauncher.launch(helper.getOverlaySettingsIntent())
            }
            PermissionType.NOTIFICATION -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    activityResultLauncher.launch(helper.getNotificationSettingsIntent())
                }
            }
            PermissionType.USAGE_STATS -> {
                activityResultLauncher.launch(helper.getUsageStatsSettingsIntent())
            }
            PermissionType.DEVICE_ADMIN -> {
                // Device admin usually req result but we check in onResume
                activityResultLauncher.launch(helper.getDeviceAdminIntent())
            }
        }
    }
    
    fun isPermissionGranted(type: PermissionType): Boolean {
        return when (type) {
            PermissionType.ACCESSIBILITY -> uiState.isAccessibilityEnabled
            PermissionType.OVERLAY -> uiState.isOverlayEnabled
            PermissionType.NOTIFICATION -> uiState.isNotificationEnabled
            PermissionType.DEVICE_ADMIN -> uiState.isDeviceAdminActive
            PermissionType.USAGE_STATS -> uiState.isUsageStatsGranted
        }
    }
    
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageData = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = pageData.icon,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = pageData.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = pageData.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (pageData.permissionType != null) {
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        val isGranted = isPermissionGranted(pageData.permissionType)
                        
                        Button(
                            onClick = { requestPermission(pageData.permissionType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isGranted
                        ) {
                            Icon(
                                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Security, // Using reliable icon
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isGranted) "Granted" else "Grant Permission")
                        }
                    }
                }
            }
            
            // Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Row {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(8.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (pagerState.currentPage == index) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                    }
                }
                
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            viewModel.completeOnboarding()
                            onFinish()
                        }
                    }
                ) {
                    Text(
                        if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started"
                    )
                }
            }
        }
    }
}
