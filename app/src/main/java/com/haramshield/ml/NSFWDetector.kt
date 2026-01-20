package com.haramshield.ml

import android.graphics.Bitmap
import com.haramshield.Constants
import com.haramshield.domain.model.ContentCategory
import com.haramshield.domain.model.DetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NSFW content detector using NudeNet-style TFLite model
 */
@Singleton
class NSFWDetector @Inject constructor(
    private val modelManager: ModelManager,
    private val imagePreprocessor: ImagePreprocessor
) {

    companion object {
        private const val INPUT_SIZE = 224
        private const val OUTPUT_CLASSES = 2 // Safe, NSFW
        private const val NSFW_INDEX = 1
    }

    /**
     * Detect NSFW content in a bitmap
     * @param bitmap The image to analyze
     * @param threshold Confidence threshold for violation (0.0 to 1.0)
     * @return DetectionResult with NSFW category
     */
    suspend fun detect(bitmap: Bitmap, threshold: Float = Constants.NSFW_CONFIDENCE_THRESHOLD): DetectionResult = withContext(Dispatchers.Default) {
        val interpreter = modelManager.getNsfwInterpreter()

        if (interpreter == null) {
            Timber.w("NSFW interpreter not available, returning safe result")
            return@withContext DetectionResult.noViolation()
        }

        try {
            // Preprocess the image (NudeNet expects 224x224 RGB)
            val inputBuffer = imagePreprocessor.preprocessForNsfw(bitmap, INPUT_SIZE)

            // Prepare output buffer [1, 2]
            val outputBuffer = Array(1) { FloatArray(OUTPUT_CLASSES) }

            // Run inference
            interpreter.run(inputBuffer, outputBuffer)

            // Get NSFW confidence (Index 1 is unsafe)
            val nsfwConfidence = outputBuffer[0][NSFW_INDEX]

            Timber.d("NSFW detection: confidence=$nsfwConfidence, threshold=$threshold")

            DetectionResult(
                category = ContentCategory.NSFW,
                confidence = nsfwConfidence,
                isViolation = nsfwConfidence > threshold, // Strict > check
                label = if (nsfwConfidence > threshold) "NSFW Content Detected" else "Safe"
            )
        } catch (e: Exception) {
            Timber.e(e, "NSFW detection failed")
            DetectionResult.noViolation()
        }
    }

    /**
     * Batch detect NSFW in multiple images
     * @param bitmaps The images to analyze
     * @param threshold Confidence threshold for violation (0.0 to 1.0)
     * @return A list of [DetectionResult]
     */
    suspend fun detectBatch(
        bitmaps: List<Bitmap>,
        threshold: Float = Constants.NSFW_CONFIDENCE_THRESHOLD
    ): List<DetectionResult> = withContext(Dispatchers.Default) {
        bitmaps.map { detect(it, threshold) }
    }
}
