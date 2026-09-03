package com.nadr59.sitemanager.domain.translator

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationUsageTracker @Inject constructor(
    private val preferences: SharedPreferences
) {

    companion object {
        /*
         * حماية داخلية للتطبيق.
         *
         * ليست حصة Google Cloud الرسمية.
         */
        const val MONTHLY_LIMIT = 400_000L

        private const val KEY_MONTH = "translation_usage_month"
        private const val KEY_CHARACTERS = "translation_usage_characters"
    }

    @Synchronized
    fun getUsedCharacters(): Long {

        resetIfNewMonth()

        return preferences.getLong(
            KEY_CHARACTERS,
            0L
        )
    }

    @Synchronized
    fun getRemainingCharacters(): Long {

        return (
            MONTHLY_LIMIT -
                getUsedCharacters()
            ).coerceAtLeast(0L)
    }

    @Synchronized
    fun canTranslate(
        characters: Long
    ): Boolean {

        if (characters <= 0L) {
            return true
        }

        return characters <= getRemainingCharacters()
    }

    @Synchronized
    fun recordCharacters(
        characters: Long
    ) {

        if (characters <= 0L) {
            return
        }

        resetIfNewMonth()

        val current =
            preferences.getLong(
                KEY_CHARACTERS,
                0L
            )

        preferences.edit()
            .putLong(
                KEY_CHARACTERS,
                current + characters
            )
            .apply()
    }

    private fun resetIfNewMonth() {

        val currentMonth =
            SimpleDateFormat(
                "yyyy-MM",
                Locale.US
            ).format(Date())

        val storedMonth =
            preferences.getString(
                KEY_MONTH,
                null
            )

        if (storedMonth != currentMonth) {

            preferences.edit()
                .putString(
                    KEY_MONTH,
                    currentMonth
                )
                .putLong(
                    KEY_CHARACTERS,
                    0L
                )
                .apply()
        }
    }
}
