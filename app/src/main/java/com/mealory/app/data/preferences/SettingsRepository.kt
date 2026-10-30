package com.mealory.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.mealoryDataStore by preferencesDataStore(name = "mealory_settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<MealorySettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::mapSettings)

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.OnboardingCompleted] = completed }
    }

    suspend fun setPrivacyPolicyVersionAccepted(version: Int) {
        require(version >= 0)
        dataStore.edit { it[Keys.PrivacyPolicyVersionAccepted] = version }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.ThemeMode] = mode.name }
    }

    suspend fun setPhotoQuality(quality: PhotoQuality) {
        dataStore.edit { it[Keys.PhotoQuality] = quality.name }
    }

    suspend fun setLastBackupAt(timestamp: Long?) {
        dataStore.edit { preferences ->
            if (timestamp == null) preferences.remove(Keys.LastBackupAt)
            else preferences[Keys.LastBackupAt] = timestamp
        }
    }

    suspend fun setDefaultMealTimeRanges(ranges: MealTimeRanges) {
        dataStore.edit { it[Keys.DefaultMealTimeRanges] = ranges.toJson() }
    }

    suspend fun replaceAll(settings: MealorySettings) {
        dataStore.edit { preferences ->
            preferences[Keys.OnboardingCompleted] = settings.onboardingCompleted
            preferences[Keys.PrivacyPolicyVersionAccepted] = settings.privacyPolicyVersionAccepted
            preferences[Keys.ThemeMode] = settings.themeMode.name
            preferences[Keys.PhotoQuality] = settings.photoQuality.name
            settings.lastBackupAt?.let { preferences[Keys.LastBackupAt] = it }
                ?: preferences.remove(Keys.LastBackupAt)
            preferences[Keys.DefaultMealTimeRanges] = settings.defaultMealTimeRanges.toJson()
        }
    }

    private fun mapSettings(preferences: Preferences): MealorySettings = MealorySettings(
        onboardingCompleted = preferences[Keys.OnboardingCompleted] ?: false,
        privacyPolicyVersionAccepted = preferences[Keys.PrivacyPolicyVersionAccepted] ?: 0,
        themeMode = preferences[Keys.ThemeMode].toEnumOrDefault(ThemeMode.SYSTEM),
        photoQuality = preferences[Keys.PhotoQuality].toEnumOrDefault(PhotoQuality.STANDARD),
        lastBackupAt = preferences[Keys.LastBackupAt],
        defaultMealTimeRanges = preferences[Keys.DefaultMealTimeRanges]
            ?.let { runCatching { MealTimeRanges.fromJson(it) }.getOrNull() }
            ?: MealTimeRanges.Default,
    )

    companion object {
        fun create(context: Context): SettingsRepository =
            SettingsRepository(context.applicationContext.mealoryDataStore)
    }

    private object Keys {
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val PrivacyPolicyVersionAccepted = intPreferencesKey("privacy_policy_version_accepted")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val PhotoQuality = stringPreferencesKey("photo_quality")
        val LastBackupAt = longPreferencesKey("last_backup_at")
        val DefaultMealTimeRanges = stringPreferencesKey("default_meal_time_ranges")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
