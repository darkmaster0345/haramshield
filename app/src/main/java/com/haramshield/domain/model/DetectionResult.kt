package com.haramshield.domain.model

/**
 * Result of content detection from ML models
 */
data class DetectionResult(
    /** The category of detected content */
    val category: ContentCategory,
    
    /** Confidence score from 0.0 to 1.0 */
    val confidence: Float,
    
    /** Whether this detection exceeds the violation threshold */
    val isViolation: Boolean,
    
    /** Timestamp of detection */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Optional bounding box for object detection (x, y, width, height as percentages) */
    val boundingBox: BoundingBox? = null,
    
    /** Additional metadata or label from model */
    val label: String? = null
) {
    companion object {
        fun noViolation() = DetectionResult(
            category = ContentCategory.NSFW,
            confidence = 0f,
            isViolation = false
        )
    }
}

/**
 * Bounding box for detected objects (normalized coordinates 0.0-1.0)
 */
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Combined detection results from all models
 */
data class DetectionSummary(
    val results: List<DetectionResult>,
    val hasViolation: Boolean = results.any { it.isViolation },
    val highestConfidenceViolation: DetectionResult? = results
        .filter { it.isViolation }
        .maxByOrNull { it.confidence },
    val processingTimeMs: Long = 0
)
