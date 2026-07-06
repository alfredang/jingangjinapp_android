package com.tertiaryinfotech.jingangjing

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tertiaryinfotech.jingangjing.data.SutraData
import java.time.LocalDate

/**
 * Central app state — bookmarks, reading progress, recitation counter, reader
 * preferences and the daily-reminder settings — persisted via SharedPreferences.
 * Mirrors the iOS AppStore.
 */
class AppStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("store", Context.MODE_PRIVATE)

    // Bookmarks (收藏) — insertion order preserved.
    var bookmarks by mutableStateOf(loadIntList(KEY_BOOKMARKS))
        private set

    // Reading progress
    var readChapters by mutableStateOf(loadIntList(KEY_READ).toSet())
        private set
    var lastReadChapter by mutableIntStateOf(prefs.getInt(KEY_LAST_READ, 0))
        private set

    // Recitation counter (持诵计数)
    var reciteTotal by mutableIntStateOf(prefs.getInt(KEY_RECITE_TOTAL, 0))
        private set
    var reciteToday by mutableIntStateOf(0)
        private set
    private var reciteDay: String = prefs.getString(KEY_RECITE_DAY, todayKey()) ?: todayKey()

    var dailyTarget by mutableIntStateOf(prefs.getInt(KEY_TARGET, 108))
        private set

    // Reader preferences
    var fontScale by mutableFloatStateOf(prefs.getFloat(KEY_FONT_SCALE, 1.0f))
        private set
    var speechRate by mutableFloatStateOf(prefs.getFloat(KEY_SPEECH_RATE, RATE_NORMAL))
        private set

    // Daily reminder (每日一偈)
    var dailyReminderEnabled by mutableStateOf(prefs.getBoolean(KEY_REMINDER_ON, false))
        private set
    var reminderHour by mutableIntStateOf(prefs.getInt(KEY_REMINDER_HOUR, 8))
        private set
    var reminderMinute by mutableIntStateOf(prefs.getInt(KEY_REMINDER_MINUTE, 0))
        private set

    init {
        reciteToday = if (reciteDay == todayKey()) prefs.getInt(KEY_RECITE_TODAY, 0) else 0
    }

    // --- Bookmarks ---

    fun isBookmarked(id: Int): Boolean = bookmarks.contains(id)

    fun toggleBookmark(id: Int) {
        bookmarks = if (bookmarks.contains(id)) bookmarks - id else bookmarks + id
        saveIntList(KEY_BOOKMARKS, bookmarks)
    }

    // --- Reading progress ---

    fun markRead(id: Int) {
        readChapters = readChapters + id
        lastReadChapter = id
        saveIntList(KEY_READ, readChapters.toList())
        prefs.edit().putInt(KEY_LAST_READ, id).apply()
    }

    val progress: Float
        get() = if (SutraData.chapters.isEmpty()) 0f
        else readChapters.size.toFloat() / SutraData.chapters.size

    fun resetProgress() {
        readChapters = emptySet()
        lastReadChapter = 0
        saveIntList(KEY_READ, emptyList())
        prefs.edit().putInt(KEY_LAST_READ, 0).apply()
    }

    // --- Recitation ---

    /** Register one recitation; rolls the daily count over at midnight. */
    fun recite() {
        rolloverIfNeeded()
        reciteToday += 1
        reciteTotal += 1
        prefs.edit()
            .putInt(KEY_RECITE_TODAY, reciteToday)
            .putInt(KEY_RECITE_TOTAL, reciteTotal)
            .apply()
    }

    fun resetToday() {
        reciteToday = 0
        reciteDay = todayKey()
        prefs.edit().putInt(KEY_RECITE_TODAY, 0).putString(KEY_RECITE_DAY, reciteDay).apply()
    }

    fun resetTotal() {
        reciteTotal = 0
        prefs.edit().putInt(KEY_RECITE_TOTAL, 0).apply()
    }

    fun setTarget(target: Int) {
        dailyTarget = target
        prefs.edit().putInt(KEY_TARGET, target).apply()
    }

    private fun rolloverIfNeeded() {
        val today = todayKey()
        if (reciteDay != today) {
            reciteDay = today
            reciteToday = 0
            prefs.edit().putString(KEY_RECITE_DAY, today).putInt(KEY_RECITE_TODAY, 0).apply()
        }
    }

    // --- Reader preferences ---

    fun setFontScaleValue(value: Float) {
        fontScale = value.coerceIn(FONT_MIN, FONT_MAX)
        prefs.edit().putFloat(KEY_FONT_SCALE, fontScale).apply()
    }

    fun setSpeechRateValue(value: Float) {
        speechRate = value
        prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()
    }

    // --- Daily reminder ---

    fun setReminderEnabled(enabled: Boolean) {
        dailyReminderEnabled = enabled
        prefs.edit().putBoolean(KEY_REMINDER_ON, enabled).apply()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        reminderHour = hour
        reminderMinute = minute
        prefs.edit().putInt(KEY_REMINDER_HOUR, hour).putInt(KEY_REMINDER_MINUTE, minute).apply()
    }

    private fun todayKey(): String = LocalDate.now().toString()

    private fun loadIntList(key: String): List<Int> =
        (prefs.getString(key, "") ?: "")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }

    private fun saveIntList(key: String, values: List<Int>) {
        prefs.edit().putString(key, values.joinToString(",")).apply()
    }

    companion object {
        @Volatile private var instance: AppStore? = null

        fun get(context: Context): AppStore =
            instance ?: synchronized(this) {
                instance ?: AppStore(context).also { instance = it }
            }

        /** Whether the daily reminder is on, readable without an AppStore (BootReceiver). */
        fun reminderPrefs(context: Context): Triple<Boolean, Int, Int> {
            val p = context.getSharedPreferences("store", Context.MODE_PRIVATE)
            return Triple(
                p.getBoolean(KEY_REMINDER_ON, false),
                p.getInt(KEY_REMINDER_HOUR, 8),
                p.getInt(KEY_REMINDER_MINUTE, 0),
            )
        }

        const val FONT_MIN = 0.85f
        const val FONT_MAX = 1.6f
        const val FONT_STEP = 0.1f

        // Android TextToSpeech rates for 慢 / 正常 / 快 (1.0 = engine default).
        const val RATE_SLOW = 0.7f
        const val RATE_NORMAL = 0.85f
        const val RATE_FAST = 1.05f

        private const val KEY_BOOKMARKS = "store.bookmarks"
        private const val KEY_READ = "store.readChapters"
        private const val KEY_LAST_READ = "store.lastReadChapter"
        private const val KEY_RECITE_TOTAL = "store.reciteTotal"
        private const val KEY_RECITE_TODAY = "store.reciteToday"
        private const val KEY_RECITE_DAY = "store.reciteDay"
        private const val KEY_TARGET = "store.dailyTarget"
        private const val KEY_REMINDER_ON = "store.dailyReminderEnabled"
        private const val KEY_REMINDER_HOUR = "store.reminderHour"
        private const val KEY_REMINDER_MINUTE = "store.reminderMinute"
        private const val KEY_FONT_SCALE = "readerFontScale"
        private const val KEY_SPEECH_RATE = "speechRate"
    }
}
