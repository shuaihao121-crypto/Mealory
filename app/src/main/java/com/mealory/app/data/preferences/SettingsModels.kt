package com.mealory.app.data.preferences

import org.json.JSONObject

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class PhotoQuality {
    STANDARD,
    HIGH,
}

data class MinuteRange(
    val startInclusive: Int,
    val endExclusive: Int,
) {
    init {
        require(startInclusive in 0..1_439)
        require(endExclusive in 0..1_440)
        require(startInclusive != endExclusive)
    }

    operator fun contains(minuteOfDay: Int): Boolean =
        if (startInclusive < endExclusive) {
            minuteOfDay in startInclusive until endExclusive
        } else {
            minuteOfDay >= startInclusive || minuteOfDay < endExclusive
        }
}

data class MealTimeRanges(
    val version: Int,
    val breakfast: MinuteRange,
    val lunch: MinuteRange,
    val dinner: MinuteRange,
    val lateNight: MinuteRange,
) {
    fun toJson(): String = JSONObject()
        .put("version", version)
        .put("breakfast", breakfast.toJson())
        .put("lunch", lunch.toJson())
        .put("dinner", dinner.toJson())
        .put("lateNight", lateNight.toJson())
        .toString()

    companion object {
        val Default = MealTimeRanges(
            version = 1,
            breakfast = MinuteRange(5 * 60, 10 * 60 + 30),
            lunch = MinuteRange(10 * 60 + 30, 15 * 60),
            dinner = MinuteRange(15 * 60, 21 * 60),
            lateNight = MinuteRange(21 * 60, 5 * 60),
        )

        fun fromJson(value: String): MealTimeRanges {
            val json = JSONObject(value)
            require(json.getInt("version") == 1) { "Unsupported meal time range version" }
            return MealTimeRanges(
                version = 1,
                breakfast = json.getJSONObject("breakfast").toMinuteRange(),
                lunch = json.getJSONObject("lunch").toMinuteRange(),
                dinner = json.getJSONObject("dinner").toMinuteRange(),
                lateNight = json.getJSONObject("lateNight").toMinuteRange(),
            )
        }
    }
}

data class MealorySettings(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val photoQuality: PhotoQuality = PhotoQuality.STANDARD,
    val lastBackupAt: Long? = null,
    val defaultMealTimeRanges: MealTimeRanges = MealTimeRanges.Default,
)

private fun MinuteRange.toJson(): JSONObject = JSONObject()
    .put("start", startInclusive)
    .put("end", endExclusive)

private fun JSONObject.toMinuteRange(): MinuteRange = MinuteRange(
    startInclusive = getInt("start"),
    endExclusive = getInt("end"),
)
