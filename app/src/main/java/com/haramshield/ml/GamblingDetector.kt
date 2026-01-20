package com.haramshield.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.haramshield.Constants
import com.haramshield.domain.model.ContentCategory
import com.haramshield.domain.model.DetectionResult
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamblingDetector @Inject constructor() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun detect(bitmap: Bitmap): DetectionResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val text = result.text

            var detectedKeyword: String? = null
            for (keyword in Constants.GAMBLING_KEYWORDS) {
                if (text.contains(keyword, ignoreCase = true)) {
                    detectedKeyword = keyword
                    break
                }
            }

            if (detectedKeyword != null) {
                Timber.d("Gambling detected: $detectedKeyword")
                DetectionResult(
                    isViolation = true,
                    category = ContentCategory.GAMBLING,
                    confidence = 1.0f,
                    boundingBox = null // Text doesn't give single box easily without iteration
                )
            } else {
                DetectionResult(
                    isViolation = false,
                    category = ContentCategory.GAMBLING,
                    confidence = 0f
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Gambling detection failed")
            DetectionResult(
                isViolation = false,
                category = ContentCategory.GAMBLING,
                confidence = 0f
            )
        }
    }
}
