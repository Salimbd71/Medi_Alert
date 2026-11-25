package com.example.data.model

enum class MedicineType {
    TABLET,
    CAPSULE,
    SYRUP,
    INJECTION
}

enum class MealTiming {
    BEFORE_FOOD,
    AFTER_FOOD,
    WITH_FOOD,
    ANYTIME
}

enum class DoseStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED,
    SNOOZED
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    ENGLISH,
    BANGLA
}

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class TimeOfDayBucket {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}
