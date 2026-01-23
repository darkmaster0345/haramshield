package com.haramshield.domain.model

/**
 * Categories of prohibited content that can be detected
 */
enum class ContentCategory(val displayName: String) {
    NSFW("NSFW Content"),
    ALCOHOL("Alcohol"),
    TOBACCO("Tobacco"),
    GAMBLING("Gambling"),
    DIET("Dietary (Pork/Haram)"),
    SCREEN_BLOCKED("Screen Blocked"),
    UNKNOWN("Unknown");
    
    companion object {
        fun fromString(value: String): ContentCategory? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
