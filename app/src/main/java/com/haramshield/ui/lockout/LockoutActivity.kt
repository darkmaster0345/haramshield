package com.haramshield.ui.lockout

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haramshield.Constants
import com.haramshield.R
import com.haramshield.ui.theme.HaramShieldTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class LockoutActivity : ComponentActivity() {

    private var remainingTimeMs: Long = 0L
    private var packageName: String? = null
    private var category: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract data from intent
        packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE_NAME)
        remainingTimeMs = intent.getLongExtra(Constants.EXTRA_REMAINING_TIME, 15 * 60 * 1000)
        category = intent.getStringExtra(Constants.EXTRA_CATEGORY)

        setContent {
            HaramShieldTheme(darkTheme = true, dynamicColor = false) {
                LockoutScreen(
                    initialTimeMs = remainingTimeMs,
                    category = category ?: "Prohibited Content",
                    onGoHome = { goToHome() }
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, true) {
            // Prevent back press - must use home button
            goToHome()
        }
    }

    private fun goToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LockoutScreen(
    initialTimeMs: Long,
    category: String,
    onGoHome: () -> Unit
) {
    var remainingTimeMs by remember { mutableStateOf(initialTimeMs) }

    // Countdown timer
    LaunchedEffect(initialTimeMs) {
        object : CountDownTimer(initialTimeMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMs = millisUntilFinished
            }

            override fun onFinish() {
                remainingTimeMs = 0
            }
        }.start()
    }

    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTimeMs) % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a1a),
                        Color(0xFF2d1f1f),
                        Color(0xFF1a1a1a)
                    )
                )
            )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 500)
            ) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Lock icon
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Color(0xFFE53935)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Determine UI Text based on Category
                val (title, message) = when {
                    category.contains("NSFW", ignoreCase = true) || category.contains("Gambling", ignoreCase = true) ->
                        "MODESTY PROTECTION" to "This content has been blocked to protect your spiritual integrity."
                    category.contains("Alcohol", ignoreCase = true) || category.contains("Tobacco", ignoreCase = true) ->
                        "HEALTH PROTECTION" to "Substance references detected. Access paused for a brief reflection."
                    category.contains("Tamper", ignoreCase = true) ->
                        "SECURITY ALERT" to "Unauthorized attempt to disable protection. System locked."
                    else ->
                        "APP LOCKED" to "This app has been locked due to detected prohibited content."
                }

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (title == "SECURITY ALERT") Color(0xFFFF5252) else Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category
                Surface(
                    color = Color(0xFFE53935).copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFFE53935),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Timer
                Text(
                    text = "Time Remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = timeString,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingTimeMs > 60000) Color.White else Color(0xFFE53935)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Home button
                Button(
                    onClick = onGoHome,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return Home")
                }

                if (remainingTimeMs == 0L) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Lock has expired. You may now use this app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center
                    )
                }
            } // End Column
        } // End AnimatedVisibility
    } // End Box
}
