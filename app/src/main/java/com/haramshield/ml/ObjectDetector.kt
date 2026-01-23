package com.haramshield.ml

import android.content.Context
import android.graphics.Bitmap
import com.haramshield.Constants
import com.haramshield.domain.model.BoundingBox
import com.haramshield.domain.model.ContentCategory
import com.haramshield.domain.model.DetectionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Object detector using TFLite Task Library for EfficientDet-Lite0
 */
@Singleton
class ObjectDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePreprocessor: ImagePreprocessor // Kept for consistency but Task Lib handles some preprocessing
) {

    private var detector: ObjectDetector? = null

    companion object {
        // EfficientDet-Lite0 typically uses 320x320
        // Task Library handles resizing internally if metadata is present, but we should be efficient
        private const val MODEL_FILE = Constants.MODEL_OBJECT_DETECTION
    }

    private fun setupDetector() {
        if (detector != null) return

        try {
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(Constants.OBJECT_CONFIDENCE_THRESHOLD) // Strict 0.80f
                .setMaxResults(10)
                .setBaseOptions(BaseOptions.builder().useGpu().build())
                .build()

            // We need to load from assets manually or use createFromFile if context is available
            // Since the model is in assets, createFromFile is easiest
            detector = ObjectDetector.createFromFileAndOptions(
                context,
                MODEL_FILE,
                options
            )
            Timber.d("EfficientDet ObjectDetector initialized with threshold ${Constants.OBJECT_CONFIDENCE_THRESHOLD}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ObjectDetector")
            // Fallback to CPU if GPU fails
            try {
                val options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setScoreThreshold(Constants.OBJECT_CONFIDENCE_THRESHOLD)
                    .setMaxResults(10)
                    .setBaseOptions(BaseOptions.builder().build())
                    .build()
                detector = ObjectDetector.createFromFileAndOptions(
                    context,
                    MODEL_FILE,
                    options
                )
                Timber.d("EfficientDet ObjectDetector initialized on CPU")
            } catch (e2: Exception) {
                Timber.e(e2, "Failed to initialize ObjectDetector fallback")
            }
        }
    }

    suspend fun detect(
        bitmap: Bitmap,
        alcoholThreshold: Float = Constants.OBJECT_CONFIDENCE_THRESHOLD, // Unused param but kept for signature compatibility
        tobaccoThreshold: Float = Constants.OBJECT_CONFIDENCE_THRESHOLD
    ): List<DetectionResult> = withContext(Dispatchers.Default) {
        if (detector == null) {
            setupDetector()
        }

        val localDetector = detector ?: run {
            Timber.w("ObjectDetector is not initialized. Skipping detection.")
            return@withContext emptyList()
        }

        try {
            // Task Library expects TensorImage
            val tensorImage = TensorImage.fromBitmap(bitmap)

            // Run inference
            val results = localDetector.detect(tensorImage)

            val detectionResults = mutableListOf<DetectionResult>()

            for (result in results) {
                // Task Library filtering by threshold is already done via options, but we double check
                // Categories loop
                for (category in result.categories) {
                    val label = category.label
                    val score = category.score

                    // Extra strict check just in case
                    if (score < Constants.OBJECT_CONFIDENCE_THRESHOLD) continue

                    var finalCategory: ContentCategory? = null

                    // Strict Target matching
                    if (Constants.TARGET_OBJECTS.any { label.contains(it, ignoreCase = true) }) {
                        finalCategory = ContentCategory.ALCOHOL // Mapping to Alcohol/Haram items
                    }

                    if (finalCategory != null) {
                        detectionResults.add(
                            DetectionResult(
                                category = finalCategory,
                                confidence = score,
                                isViolation = true,
                                label = label,
                                boundingBox = BoundingBox(
                                    x = result.boundingBox.left,
                                    y = result.boundingBox.top,
                                    width = result.boundingBox.width(),
                                    height = result.boundingBox.height()
                                )
                            )
                        )
                    }
                }
            }

            if (detectionResults.isNotEmpty()) {
                Timber.d("Object detection violation: ${detectionResults.size} detected")
            }

            detectionResults

        } catch (e: Exception) {
            Timber.e(e, "Object detection error")
            emptyList()
        }
    }

    suspend fun containsAlcohol(bitmap: Bitmap, threshold: Float = Constants.OBJECT_CONFIDENCE_THRESHOLD): Boolean {
        return detect(bitmap).isNotEmpty()
    }

    suspend fun containsTobacco(bitmap: Bitmap, threshold: Float = Constants.OBJECT_CONFIDENCE_THRESHOLD): Boolean {
        return false // Not targeted
    }
}
