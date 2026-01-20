package com.haramshield

/**
 * App-wide constants for HaramShield
 */
object Constants {
    
    // App Configuration
    const val APP_NAME = "HaramShield"
    const val PACKAGE_NAME = "com.haramshield"
    
    // Lockout Configuration
    const val DEFAULT_LOCKOUT_DURATION_MINUTES = 15L
    const val MIN_LOCKOUT_DURATION_MINUTES = 5L
    const val MAX_LOCKOUT_DURATION_MINUTES = 60L
    
    // Detection Configuration
    const val DEFAULT_NSFW_THRESHOLD = 0.7f
    const val DEFAULT_OBJECT_THRESHOLD = 0.6f
    const val MIN_DETECTION_THRESHOLD = 0.3f
    const val MAX_DETECTION_THRESHOLD = 0.95f
    
    // Screenshot Configuration
    const val DEFAULT_SCREENSHOT_INTERVAL_MS = 2000L
    const val MIN_SCREENSHOT_INTERVAL_MS = 500L
    const val MAX_SCREENSHOT_INTERVAL_MS = 10000L
    
    // Notification IDs
    const val NOTIFICATION_ID_MONITORING = 1001
    const val NOTIFICATION_ID_SCREEN_CAPTURE = 1002
    const val NOTIFICATION_ID_LOCKOUT = 1003
    const val NOTIFICATION_ID_VIOLATION = 1004
    
    // Notification Channels
    const val CHANNEL_ID_MONITORING = "monitoring_service"
    const val CHANNEL_ID_LOCKOUT = "lockout_alerts"
    const val CHANNEL_ID_VIOLATION = "violation_alerts"
    
    // Intent Actions
    const val ACTION_RESTART_SERVICE = "com.haramshield.RESTART_SERVICE"
    const val ACTION_STOP_MONITORING = "com.haramshield.STOP_MONITORING"
    const val ACTION_OPEN_SETTINGS = "com.haramshield.OPEN_SETTINGS"
    const val ACTION_SNOOZE = "com.haramshield.SNOOZE"
    const val ACTION_DISABLE_SERVICE = "com.haramshield.DISABLE_SERVICE"
    
    // Intent Extras
    const val EXTRA_PACKAGE_NAME = "extra_package_name"
    const val EXTRA_LOCKOUT_UNTIL = "extra_lockout_until"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_REMAINING_TIME = "extra_remaining_time"
    
    // Preferences Keys
    const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
    const val PREF_MONITORING_ENABLED = "monitoring_enabled"
    const val PREF_PIN_SET = "pin_set"
    
    // WorkManager Tags
    const val WORK_TAG_KEEP_ALIVE = "keep_alive_worker"
    const val WORK_TAG_CLEANUP = "cleanup_worker"
    
    // WorkManager Configuration
    const val KEEP_ALIVE_INTERVAL_MINUTES = 15L
    
    // ML Model Files
    const val MODEL_NSFW = "nsfw_model.tflite"
    const val MODEL_OBJECT_DETECTION = "object_detection.tflite"
    const val LABELS_OBJECT_DETECTION = "labels.txt"
    
    // Database
    const val DATABASE_NAME = "haramshield_database"
    const val DATABASE_VERSION = 1
    
    // Violation Log Retention
    const val VIOLATION_LOG_RETENTION_DAYS = 30
    
    // Hard Block Configuration
    const val NSFW_CONFIDENCE_THRESHOLD = 0.60f
    const val OBJECT_CONFIDENCE_THRESHOLD = 0.80f
    
    val TARGET_OBJECTS = setOf("bottle", "wine glass", "cup")
    
    // CASINO / GAMBLING (13 Keywords)
    val GAMBLING_KEYWORDS = setOf(
        "Casino", "Bet", "Jackpot", "Slot", "Poker", "Lottery", "Wager", "Roulette",
        "Blackjack", "Sportsbook", "DraftKings", "FanDuel", "Baccarat"
    )
    
    // INTOXICANTS (20 Keywords)
    val INTOXICANTS_KEYWORDS = setOf(
        "Marlboro", "Vape", "Tobacco", "Cigarette", "Juul", "Smoking", "Nicotine", "Cigar",
        "Alcohol", "Wine", "Liquor", "Beer", "Dispensary", "Weed", "Hookah",
        "Cannabis", "Marijuana", "Vodka", "Whiskey", "Tequila"
    )
    
    // NSFW / OBSCENE (15 Keywords)
    val NSFW_TEXT_KEYWORDS = setOf(
        "Porn", "Nude", "Xxx", "Sexy", "Erotica", "OnlyFans", "Camgirl", "Hentai",
        "Sex", "Milf", "Teen", "Adult", "Escort", "Strip", "Naked"
    )
    
    // ANTI-ISLAMIC / BLASPHEMY (8 Keywords)
    val ANTI_ISLAMIC_KEYWORDS = setOf(
        "Blasphemy", "Atheism", "Anti-Islam", "Shirk", "Infidel", "Apostate", "Quran Burning", "Draw Muhammad"
    )
    
    val ALL_HARAM_KEYWORDS = GAMBLING_KEYWORDS + INTOXICANTS_KEYWORDS + NSFW_TEXT_KEYWORDS + ANTI_ISLAMIC_KEYWORDS
    
    // Thresholds & Intervals (Ihsan Execution Optimization)
    const val TOBACCO_CONFIDENCE_THRESHOLD = 0.65f // Lowered to 0.65f
    
    const val SCREENSHOT_INTERVAL_HIGH_POWER = 800L  // 800ms
    const val SCREENSHOT_INTERVAL_LOW_POWER = 2500L  // 2500ms
    
    const val DEACTIVATE_DELAY_MS = 60000L // 60 Seconds
    const val ACTION_NOOR_PULSE = "com.haramshield.ACTION_NOOR_PULSE"
}
