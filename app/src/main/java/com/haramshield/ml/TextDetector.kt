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

import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect

/**
 * Text detector using ML Kit Text Recognition to find prohibited keywords (e.g., Tobacco brands)
 */
@Singleton
class TextDetector @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val settingsManager: com.haramshield.data.preferences.SettingsManager
) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Unified keyword map
    private val allBlockedKeywords = mutableMapOf<String, ContentCategory>()
    private var customBlockedWords = setOf<String>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            loadAllKeywords()
        }

        // Observe Custom Words
        scope.launch {
            settingsManager.customBlockedWords.collect { 
                customBlockedWords = it
                Timber.d("Updated custom blocked words: ${it.size}")
            }
        }
    }

    private fun loadAllKeywords() {
        // 1. Load from Constants
        Constants.INTOXICANTS_KEYWORDS.forEach { allBlockedKeywords[it.lowercase()] = ContentCategory.TOBACCO }
        Constants.GAMBLING_KEYWORDS.forEach { allBlockedKeywords[it.lowercase()] = ContentCategory.GAMBLING }
        Constants.NSFW_TEXT_KEYWORDS.forEach { allBlockedKeywords[it.lowercase()] = ContentCategory.NSFW }
        Constants.ANTI_ISLAMIC_KEYWORDS.forEach { allBlockedKeywords[it.lowercase()] = ContentCategory.NSFW } // Mapped to NSFW as per previous logic

        // 2. Load from asset file
        try {
            Timber.d("Loading asset blocklist...")
            context.assets.open("haram_blocklist.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split(":", limit = 2)
                        if (parts.size == 2) {
                            val categoryString = parts[0].trim().uppercase()
                            val category = ContentCategory.fromString(categoryString)
                            val words = parts[1].split(",").map { it.trim().lowercase() }
                            if (category != null) {
                                words.forEach { word -> allBlockedKeywords[word] = category }
                            }
                        }
                    }
                }
            }
            Timber.d("Loaded ${allBlockedKeywords.size} total keywords")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load blocklist asset")
        }
    }

    /**
     * Detect prohibited text in a bitmap.
     * Checks against a unified list of keywords.
     */
    suspend fun detectProhibitedText(bitmap: Bitmap): DetectionResult {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val text = result.text.lowercase()

            // Check unified keyword map
            for ((keyword, category) in allBlockedKeywords) {
                if (isKeywordDetected(text, keyword)) {
                    Timber.d("Haram keyword detected: $keyword, Category: $category")
                    return DetectionResult(
                        category = category,
                        confidence = 1.0f,
                        isViolation = true,
                        label = "Haram Text: $keyword"
                    )
                }
            }

            // Check CUSTOM Words (still separate for now)
            for (keyword in customBlockedWords) {
                if (isKeywordDetected(text, keyword)) {
                    Timber.d("Custom keyword detected: $keyword")
                    return DetectionResult(
                        category = ContentCategory.NSFW, // Default category for custom words
                        confidence = 1.0f,
                        isViolation = true,
                        label = "Custom Block: $keyword"
                    )
                }
            }

            return DetectionResult.noViolation()

        } catch (e: Exception) {
            Timber.e(e, "Text detection failed")
            return DetectionResult.noViolation()
        }
    }

    private fun isKeywordDetected(fullText: String, keyword: String): Boolean {
        return fullText.contains(keyword.lowercase())
    }
}
