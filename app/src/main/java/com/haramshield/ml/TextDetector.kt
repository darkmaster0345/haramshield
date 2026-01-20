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
    
    // Additional Blocklists
    private val assetBlockedWords = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private var customBlockedWords = setOf<String>()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Load Asset Blocklist
        scope.launch {
            try {
                Timber.d("Loading asset blocklist...")
                context.assets.open("haram_blocklist.txt").bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            // Split by comma if present, otherwise just the line
                            val words = trimmed.split(",").map { it.trim().lowercase() }
                            assetBlockedWords.addAll(words)
                        }
                    }
                }
                Timber.d("Loaded ${assetBlockedWords.size} words from asset blocklist")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load blocklist asset")
            }
        }
        
        // Observe Custom Words
        scope.launch {
            settingsManager.customBlockedWords.collect { 
                customBlockedWords = it
                Timber.d("Updated custom blocked words: ${it.size}")
            }
        }
    }

    /**
     * Detect prohibited text in a bitmap.
     * Checks against Haram Word List (Intoxicants, Gambling, NSFW, Anti-Islamic) + Custom + Assets.
     */
    suspend fun detectProhibitedText(bitmap: Bitmap): DetectionResult {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val text = result.text.lowercase()
            
            // Universal Case-Insensitive OCR
            // We iterate through categories to maintain specific labeling, but ensure lowercase comparison.
            
            // Check Intoxicants
            for (keyword in Constants.INTOXICANTS_KEYWORDS) {
                if (isKeywordDetected(text, keyword)) {
                    Timber.d("Intoxicant keyword detected: $keyword")
                    return DetectionResult(
                        category = ContentCategory.TOBACCO,
                        confidence = 1.0f,
                        isViolation = true,
                        label = "Intoxicant: $keyword"
                    )
                }
            }
            
            // Check Gambling
            for (keyword in Constants.GAMBLING_KEYWORDS) {
                if (isKeywordDetected(text, keyword)) {
                    Timber.d("Gambling keyword detected: $keyword")
                    return DetectionResult(
                        category = ContentCategory.GAMBLING,
                        confidence = 1.0f,
                        isViolation = true,
                        label = "Gambling: $keyword"
                    )
                }
            }
            
            // Check NSFW Text
            for (keyword in Constants.NSFW_TEXT_KEYWORDS) {
                if (isKeywordDetected(text, keyword)) {
                    Timber.d("NSFW keyword detected: $keyword")
                    return DetectionResult(
                        category = ContentCategory.NSFW,
                        confidence = 1.0f,
                        isViolation = true,
                        label = "NSFW Text: $keyword"
                    )
                }
            }
            
            // Check Anti-Islamic
            for (keyword in Constants.ANTI_ISLAMIC_KEYWORDS) {
                if (isKeywordDetected(text, keyword)) {
                    Timber.d("Anti-Islamic keyword detected: $keyword")
                    return DetectionResult(
                        category = ContentCategory.NSFW,
                        confidence = 1.0f,
                        isViolation = true,
                        label = "Anti-Islamic: $keyword"
                    )
                }
            }
            
            // Check CUSTOM Words
            for (keyword in customBlockedWords) {
                if (isKeywordDetected(text, keyword)) {
                    Timber.d("Custom keyword detected: $keyword")
                    return DetectionResult(
                        category = ContentCategory.NSFW,
                        confidence = 1.0f,
                        isViolation = true,
                        label = "Custom Block: $keyword"
                    )
                }
            }
            
            // Check ASSET Words (Optimized with synchronized iterator)
             synchronized(assetBlockedWords) {
                for (keyword in assetBlockedWords) {
                    if (isKeywordDetected(text, keyword)) {
                         Timber.d("Asset keyword detected: $keyword")
                         return DetectionResult(
                             category = ContentCategory.NSFW,
                             confidence = 1.0f,
                             isViolation = true,
                             label = "Haram List: $keyword"
                         )
                    }
                }
            }
            
            return DetectionResult.noViolation()
            
        } catch (e: Exception) {
            Timber.e(e, "Text detection failed")
            return DetectionResult.noViolation()
        }
    }
    
    /**
     * Smart Detection Logic:
     * - If word is short (< 4 chars), enforce Whole Word Matching (Boundaries).
     *   Prevents "General" triggering "ale" or "Vegetable" triggering "bet".
     * - If word is long, standard containment is fine (and robust against spacing issues).
     */
    private fun isKeywordDetected(fullText: String, keyword: String): Boolean {
        val lowerText = fullText.lowercase() // Optimization: text is already lowercased in caller, but safety first or pass it in
        // Wait, caller 'text' IS lowercased. 'keyword' might not be.
        // Let's rely on args being correct or handle it here.
        // The caller passes 'text' which is result.text.lowercase().
        // So fullText is ALREADY lowercase.
        
        val cleanKeyword = keyword.lowercase()
        
        return if (cleanKeyword.length < 4) {
            // Regex for Whole Word Boundary
            // \bKEYWORD\b
            try {
                // Escape regex chars in keyword just in case (e.g. s.e.x)
                val regex = Regex("\\b${Regex.escape(cleanKeyword)}\\b")
                regex.containsMatchIn(fullText)
            } catch (e: Exception) {
                // Fallback if regex fails (unlikely)
                fullText.contains(cleanKeyword)
            }
        } else {
            // Standard check
            fullText.contains(cleanKeyword)
        }
    }
}
