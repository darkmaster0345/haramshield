package com.haramshield.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * HaramShieldService - The Brain
 * 
 * Accessibility service that captures screen every 500ms and detects:
 * 1. NSFW content using NSFW.tflite model
 * 2. Haram words via ML Kit OCR
 * 
 * Protection: Sends user HOME on any violation.
 */
class HaramShieldService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainExecutor: Executor = Executors.newSingleThreadExecutor()

    // TensorFlow Lite
    private var interpreter: Interpreter? = null
    private var imageProcessor: ImageProcessor? = null
    private var labelMap: List<String> = emptyList()

    // ML Kit OCR
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Blocked Words List
    private val blockedWords = listOf(
        "porn", "xxx", "hentai", "nsfw", "sex", "nude", "naked", 
        "boobs", "pussy", "cock", "dick", "fuck", "onlyfans", 
        "xvideos", "pornhub", "xnxx", "brazzers", "xhamster"
    )

    // Constants
    private val NSFW_THRESHOLD = 0.6f
    private val CAPTURE_INTERVAL_MS = 500L
    private val MODEL_FILE = "NSFW.tflite"
    private val DICT_FILE = "dict.txt"

    // State
    @Volatile
    private var isCapturing = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("HaramShieldService created")
        initializeModel()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("HaramShieldService connected - starting protection")
        startScreenCaptureLoop()
    }

    /**
     * Initialize TensorFlow Lite model from assets
     */
    private fun initializeModel() {
        try {
            // Load model
            val modelBuffer = loadModelFile(MODEL_FILE)
            interpreter = Interpreter(modelBuffer)
            Timber.d("NSFW.tflite loaded successfully")

            // Load label map from dict.txt
            labelMap = loadLabelMap(DICT_FILE)
            Timber.d("Label map loaded: $labelMap")

            // Setup image processor (224x224, normalize to [0,1])
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

        } catch (e: Exception) {
            Timber.e(e, "ERROR: Failed to load NSFW.tflite from assets. Model file may be missing!")
        }
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(fileName)
        return FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    private fun loadLabelMap(fileName: String): List<String> {
        val labels = mutableListOf<String>()
        try {
            assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.trim()?.takeIf { it.isNotEmpty() }?.let { labels.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load dict.txt")
        }
        return labels
    }

    /**
     * Start the 500ms screen capture loop
     */
    private fun startScreenCaptureLoop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Timber.w("takeScreenshot API requires Android R (API 30)+. Device is running API ${Build.VERSION.SDK_INT}")
            return
        }

        isCapturing = true
        serviceScope.launch {
            Timber.d("Screen capture loop started (${CAPTURE_INTERVAL_MS}ms interval)")
            while (isCapturing) {
                captureAndAnalyze()
                delay(CAPTURE_INTERVAL_MS)
            }
        }
    }

    /**
     * Capture screen and run analysis
     */
    private fun captureAndAnalyze() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val bitmap = Bitmap.wrapHardwareBuffer(
                        screenshot.hardwareBuffer,
                        screenshot.colorSpace
                    )
                    
                    if (bitmap != null) {
                        // Convert to software bitmap for TFLite processing
                        val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        bitmap.recycle()
                        screenshot.hardwareBuffer.close()
                        
                        serviceScope.launch {
                            analyzeScreen(softwareBitmap)
                        }
                    } else {
                        screenshot.hardwareBuffer.close()
                    }
                }

                override fun onFailure(errorCode: Int) {
                    Timber.w("Screenshot failed with error code: $errorCode")
                }
            }
        )
    }

    /**
     * Analyze screenshot for NSFW content and haram words
     */
    private suspend fun analyzeScreen(bitmap: Bitmap) {
        try {
            var violation = false
            var reason = ""

            // Run NSFW detection and OCR in parallel
            val nsfwResult = runNsfwDetection(bitmap)
            val ocrResult = runOcrDetection(bitmap)

            // Check NSFW score (index 1 = 'nude' in dict.txt)
            if (nsfwResult.nudeScore > NSFW_THRESHOLD) {
                violation = true
                reason = "NSFW detected (score: ${nsfwResult.nudeScore})"
                Timber.w("VIOLATION: $reason")
            }

            // Check blocked words
            if (ocrResult.foundWord != null) {
                violation = true
                reason = "Haram word detected: '${ocrResult.foundWord}'"
                Timber.w("VIOLATION: $reason")
            }

            // THE SHIELD: Trigger protection
            if (violation) {
                Timber.w("SHIELD ACTIVATED! Sending user HOME. Reason: $reason")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error analyzing screen")
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Run NSFW.tflite model inference
     */
    private fun runNsfwDetection(bitmap: Bitmap): NsfwResult {
        val currentInterpreter = interpreter ?: return NsfwResult(0f)
        val processor = imageProcessor ?: return NsfwResult(0f)

        try {
            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            val processedImage = processor.process(tensorImage)

            // Output: [1, 2] for [nonnude, nude]
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)
            currentInterpreter.run(processedImage.buffer, outputBuffer.buffer.rewind())

            val probabilities = outputBuffer.floatArray
            
            // Index mapping from dict.txt: 0=nonnude, 1=nude
            val nonNudeScore = probabilities.getOrElse(0) { 0f }
            val nudeScore = probabilities.getOrElse(1) { 0f }
            
            Timber.d("NSFW Scores - NonNude: $nonNudeScore, Nude: $nudeScore")
            
            return NsfwResult(nudeScore)

        } catch (e: Exception) {
            Timber.e(e, "NSFW inference failed")
            return NsfwResult(0f)
        }
    }

    /**
     * Run ML Kit OCR to find blocked words
     */
    private suspend fun runOcrDetection(bitmap: Bitmap): OcrResult {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            val detectedText = result.text.lowercase()

            for (word in blockedWords) {
                if (detectedText.contains(word)) {
                    return OcrResult(word)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "OCR detection failed")
        }
        return OcrResult(null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used - we use screen capture loop instead
    }

    override fun onInterrupt() {
        Timber.d("HaramShieldService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isCapturing = false
        serviceScope.cancel()
        interpreter?.close()
        textRecognizer.close()
        Timber.d("HaramShieldService destroyed")
    }

    // Data classes for results
    private data class NsfwResult(val nudeScore: Float)
    private data class OcrResult(val foundWord: String?)
}
