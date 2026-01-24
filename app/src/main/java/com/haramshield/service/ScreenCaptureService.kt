package com.haramshield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.haramshield.Constants
import com.haramshield.R
import com.haramshield.data.preferences.SettingsManager
import com.haramshield.domain.model.DetectionSummary
import com.haramshield.domain.usecase.ProcessDetectionUseCase
import com.haramshield.domain.usecase.ProcessingResult
import com.haramshield.ml.Classifier
import com.haramshield.ml.HaramType
import com.haramshield.ml.GamblingDetector
// import com.haramshield.ml.NSFWDetector // Replaced by Classifier
// import com.haramshield.ml.ObjectDetector // Replaced by Classifier (partially)
import com.haramshield.ui.MainActivity
import com.haramshield.ui.lockout.LockoutActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ScreenCaptureService : Service() {
    
    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var classifier: Classifier // The new HaramShield Core
    // @Inject lateinit var nsfwDetector: NSFWDetector
    // @Inject lateinit var objectDetector: ObjectDetector // TODO: Check if still needed for other objects?
                                                        // For now assuming Classifier covers 
                                                        // NSFW, Alcohol, Pork. 
                                                        // Keeping logical structure if we want to add back specific object detector later.
    @Inject lateinit var gamblingDetector: GamblingDetector
    @Inject lateinit var textDetector: com.haramshield.ml.TextDetector
    @Inject lateinit var processDetectionUseCase: ProcessDetectionUseCase
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureJob: Job? = null
    private var currentPackage: String? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var mediaProjection: MediaProjection? = null
    private var isMediaProjectionInitialized = false
    private lateinit var captureReceiver: BroadcastReceiver
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("ScreenCaptureService created")
        
        // Initialize broadcast receiver for capture commands from AccessibilityService
        captureReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    MonitoringAccessibilityService.ACTION_START_CAPTURE -> {
                        val packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE_NAME)
                        currentPackage = packageName
                        Timber.d("Received capture request for: $packageName")
                        if (isMediaProjectionInitialized && (captureJob == null || !captureJob!!.isActive)) {
                            startCapturing()
                        }
                    }
                }
            }
        }
        
        // Register the receiver
        val filter = IntentFilter(MonitoringAccessibilityService.ACTION_START_CAPTURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(captureReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(captureReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        // FOREGROUND SERVICE: Immediate promotion to prevent killing
        startForeground(
            Constants.NOTIFICATION_ID_SCREEN_CAPTURE,
            createNotification()
        )
        
        when (action) {
            ACTION_INIT_PROJECTION -> {
                // Initialize MediaProjection from permission result
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != -1 && resultData != null) {
                    initializeMediaProjection(resultCode, resultData)
                } else {
                    Timber.e("Failed to get MediaProjection result data")
                }
            }
            MonitoringAccessibilityService.ACTION_START_CAPTURE -> {
                val packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE_NAME)
                currentPackage = packageName
                if (isMediaProjectionInitialized && (captureJob == null || !captureJob!!.isActive)) {
                    startCapturing()
                }
            }
            Constants.ACTION_STOP_MONITORING -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        // GHOST SERVICE FIX: Restart if killed
        return START_STICKY
    }
    
    /**
     * Initialize MediaProjection from permission grant result.
     * Must be called after user grants screen capture permission.
     */
    private fun initializeMediaProjection(resultCode: Int, data: Intent) {
        try {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            if (mediaProjection != null) {
                setupVirtualDisplay()
                isMediaProjectionInitialized = true
                Timber.d("MediaProjection initialized successfully")
            } else {
                Timber.e("MediaProjection is null after initialization")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize MediaProjection")
        }
    }
    
    /**
     * Set up VirtualDisplay for screen capture.
     */
    private fun setupVirtualDisplay() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "HaramShield_Screen",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            Handler(Looper.getMainLooper())
        )
        
        Timber.d("VirtualDisplay created: ${width}x${height} @ ${density}dpi")
    }

    private fun createNotification(): Notification {
        val channelId = Constants.CHANNEL_ID_MONITORING
        val channelName = "Active Shield Protection"
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW 
            ).apply {
                description = "Keeps the HaramShield engine running in the background."
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("HaramShield Active")
            .setContentText("Scanning for prohibited content...")
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this icon exists, fallback to launcher
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private suspend fun captureAndAnalyze(frameCount: Long) {
        val bitmap = captureScreen() ?: return
        val packageName = currentPackage ?: return
        
        try {
            // Hard coded thresholds are now in detectors via Constants default params
            // But we can pass specific ones if we were reading from settings. 
            // User requested HARD CODED logic, so we rely on the detectors using Constants.*
            
            // Start with empty results
            val allResults = mutableListOf<com.haramshield.domain.model.DetectionResult>()
            var isMainViolation = false
            
            // CHECK FOR BLACK SCREEN (Incognito / Secure Window)
            if (isBitmapBlackOrTransparent(bitmap)) {
                Timber.w("Black/Transparent screen detected - Suspecting Incognito/Secure content")
                val blockedResult = com.haramshield.domain.model.DetectionResult(
                    category = com.haramshield.domain.model.ContentCategory.SCREEN_BLOCKED,
                    confidence = 1.0f,
                    isViolation = true,
                    label = "Secure Content / Incognito"
                )
                allResults.add(blockedResult)
            } else {
                // Only run expensive ML if we can actually see the screen
                
                // Run detections
                // Haram Core: Run EVERY frame (400ms)
                val haramType = classifier.checkScreen(bitmap)
                
                if (haramType != HaramType.SAFE) {
                    isMainViolation = true
                    Timber.d("HaramShield Core Violation: $haramType")
                    
                    val category = when(haramType) {
                        HaramType.NSFW -> com.haramshield.domain.model.ContentCategory.NSFW
                        HaramType.ALCOHOL -> com.haramshield.domain.model.ContentCategory.ALCOHOL
                        HaramType.PORK -> com.haramshield.domain.model.ContentCategory.DIET
                        HaramType.SAFE -> com.haramshield.domain.model.ContentCategory.UNKNOWN // Should not happen
                    }
                    
                    allResults.add(com.haramshield.domain.model.DetectionResult(
                        category = category,
                        confidence = 0.90f, // > 0.85 threshold logic is inside Classifier
                        isViolation = true,
                        label = "Detected ${haramType.name}"
                    ))
                }

                // Gambling:
                val gamblingResult = gamblingDetector.detect(bitmap)
                
                // Text/OCR: Run EVERY frame (Optimization Removed for Robustness)
                // User requested "Every frame" for strictness
                val textResult = textDetector.detectProhibitedText(bitmap)
                
                allResults.add(gamblingResult)
                allResults.add(textResult)
            }
            
            val summary = DetectionSummary(results = allResults)
            
            if (summary.hasViolation) {
                Timber.w("Violation detected in $packageName: ${summary.highestConfidenceViolation}")
                
                // CRITICAL RULE 1: If Haram Violation, trigger Global Home immediately
                // The "Kick" Command
                if (isMainViolation || (summary.highestConfidenceViolation?.confidence ?: 0f) >= 0.99f) {
                    Timber.w("CRITICAL: Violation detected, forcing HOME action immediately.")
                    val homeIntent = Intent(MonitoringAccessibilityService.ACTION_PERFORM_GLOBAL_HOME)
                    homeIntent.setPackage(Constants.PACKAGE_NAME) // Send to our own app
                    sendBroadcast(homeIntent)
                }
                
                // Process violation (Log & Lock in DB)
                val result = processDetectionUseCase(packageName, null, summary)
                
                if (result is ProcessingResult.AppLocked) {
                    showLockoutScreen(packageName, result.lockedApp.category)
                }
            }
        } finally {
            bitmap.recycle()
        }
    }
    
    private fun showLockoutScreen(packageName: String, category: String) {
        val intent = Intent(this, LockoutActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Constants.EXTRA_PACKAGE_NAME, packageName)
            putExtra(Constants.EXTRA_CATEGORY, category)
        }
        startActivity(intent)
    }

    private fun captureScreen(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        return bitmap
    }

    private fun startCapturing() {
        captureJob?.cancel()
        captureJob = serviceScope.launch {
            // Dynamic Scanning Logic
            var frameCount = 0L
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            
            while (true) {
                // Check Power State for "Ihsan" Optimization
                val isPowerSave = powerManager.isPowerSaveMode
                
                // Dynamic Intervals based on power state
                // Optimized by Ihsan Command
                val baseInterval = if (isPowerSave) Constants.SCREENSHOT_INTERVAL_LOW_POWER else Constants.SCREENSHOT_INTERVAL_HIGH_POWER
                val settingsInterval = settingsManager.screenshotIntervalMs.first()
                
                // Respect user setting but ensure floor matches power state
                val actualInterval = maxOf(settingsInterval, baseInterval)
                
                delay(actualInterval)
                
                // THE NOOR PULSE: Broadcast scan event for UI visual
                val pulseIntent = Intent(Constants.ACTION_NOOR_PULSE)
                pulseIntent.setPackage(Constants.PACKAGE_NAME)
                sendBroadcast(pulseIntent)
                
                if (currentPackage != null) {
                    // SNOOZE CHECK: Skip detection if user paused protection
                    val isSnoozed = settingsManager.isSnoozed.first()
                    if (isSnoozed) {
                        Timber.d("SNOOZED: Skipping detection")
                    } else {
                        captureAndAnalyze(frameCount)
                        frameCount++
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        captureJob?.cancel()
        serviceScope.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        unregisterReceiver(captureReceiver)
        Timber.d("ScreenCaptureService destroyed")
    }
    
    /**
     * Checks if a bitmap is completely black or transparent.
     * This often happens when taking a screenshot of a "secure" window (Incognito mode, Banking apps).
     */
    private fun isBitmapBlackOrTransparent(bitmap: Bitmap): Boolean {
        // Optimize: Check a grid of points instead of every pixel
        // 10x10 grid is usually enough to detect a real image vs a black screen
        val width = bitmap.width
        val height = bitmap.height
        
        // If bitmap is tiny, just return false (safe)
        if (width < 50 || height < 50) return false
        
        // Check center + corners + mid-points
        val stepX = width / 10
        val stepY = height / 10
        
        for (x in stepX until width step stepX) {
            for (y in stepY until height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                
                // If we find ANY detection of a non-black/non-transparent pixel, it's a valid screen
                // Check alpha (must be full opacity) and some color
                val alpha = (pixel shr 24) and 0xff
                val red = (pixel shr 16) and 0xff
                val green = (pixel shr 8) and 0xff
                val blue = (pixel) and 0xff
                
                // If it's not effectively black (allow some noise margin e.g. < 10)
                // And not transparent
                if (alpha > 0 && (red > 10 || green > 10 || blue > 10)) {
                    return false
                }
            }
        }
        
        // If we got here, all checked pixels were black or transparent
        return true
    }

    companion object {
        const val ACTION_INIT_PROJECTION = "com.haramshield.INIT_PROJECTION"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
    }
}
