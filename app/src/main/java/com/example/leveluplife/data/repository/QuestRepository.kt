package com.example.leveluplife.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.leveluplife.data.Quest
import com.example.leveluplife.data.MoodEntry
import java.text.SimpleDateFormat
import java.util.Locale

class QuestRepository(ctx: Context) {

    private val appCtx = ctx.applicationContext

    /** Main app prefs (quests, xp, moods, badges, reminders…) */
    private val prefs: SharedPreferences =
        appCtx.getSharedPreferences("leveluplife", Context.MODE_PRIVATE)

    /** Focus Timer sessions prefs (FocusTimerFragment හි recordFocusSession() ළඟ භාවිතා වුණ) */
    private val prefsTimer: SharedPreferences =
        appCtx.getSharedPreferences("leveluplife_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    // ==== Keys (main prefs) ====
    private val QUESTS_KEY = "quests"
    private val MOODS_KEY = "moods"                // <— consistent key (migrate from "moods_list")
    private val LEVEL_KEY = "user_level"
    private val XP_KEY = "user_xp"
    private val REMINDER_ON_KEY = "reminder_on"
    private val REMINDER_MIN_KEY = "reminder_min"
    private val STREAK_KEY = "streak_count"
    private val LAST_ACTIVE_DATE_KEY = "last_active_date" // yyyy-MM-dd
    private val BADGES_KEY = "badges" // JSON array of strings

    // ==== Keys (timer prefs) ====
    private val FOCUS_DAY_KEY = "focus_sessions_day"        // yyyy-MM-dd
    private val FOCUS_COUNT_KEY = "focus_sessions_today"

    // ==== Generic helpers ====
    private inline fun <reified T> readList(key: String): MutableList<T> {
        return try {
            val json = prefs.getString(key, "[]")
            val type = object : TypeToken<MutableList<T>>() {}.type
            gson.fromJson<MutableList<T>>(json, type) ?: mutableListOf()
        } catch (_: Throwable) {
            mutableListOf()
        }
    }

    private fun writeJson(key: String, value: Any) {
        prefs.edit().putString(key, gson.toJson(value)).apply()
    }

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())

    // ==== Quests ====
    fun getAllQuests(): MutableList<Quest> = readList(QUESTS_KEY)

    fun saveAllQuests(list: MutableList<Quest>) {
        writeJson(QUESTS_KEY, list)
    }

    // ==== Moods (with migration from old "moods_list") ====
    fun getMoodHistory(): MutableList<MoodEntry> {
        val jsonNew = prefs.getString(MOODS_KEY, null)
        val jsonOld = prefs.getString("moods_list", null) // legacy key

        val sourceJson = when {
            !jsonNew.isNullOrEmpty() -> jsonNew
            !jsonOld.isNullOrEmpty() -> jsonOld
            else -> "[]"
        }

        val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
        val list = runCatching { gson.fromJson<MutableList<MoodEntry>>(sourceJson, type) }
            .getOrElse { mutableListOf() }

        // migrate to new key if came from legacy
        if (jsonNew.isNullOrEmpty() && !jsonOld.isNullOrEmpty()) {
            writeJson(MOODS_KEY, list)
            prefs.edit().remove("moods_list").apply()
        }
        return list
    }

    fun saveMoodHistory(list: List<MoodEntry>) {
        prefs.edit().putString(MOODS_KEY, gson.toJson(list)).apply()
    }

    // ==== Level/XP ====
    fun getUserLevel(): Int = prefs.getInt(LEVEL_KEY, 1)
    fun saveUserLevel(v: Int) { prefs.edit().putInt(LEVEL_KEY, v).apply() }

    fun getCurrentXp(): Int = prefs.getInt(XP_KEY, 0)
    fun saveCurrentXp(v: Int) { prefs.edit().putInt(XP_KEY, v).apply() }

    // ==== Hydration Reminder ====
    fun getReminderStatus(): Boolean = prefs.getBoolean(REMINDER_ON_KEY, false)
    fun setReminderStatus(on: Boolean) { prefs.edit().putBoolean(REMINDER_ON_KEY, on).apply() }

    fun getReminderInterval(): Int = prefs.getInt(REMINDER_MIN_KEY, 60)
    fun setReminderInterval(min: Int) { prefs.edit().putInt(REMINDER_MIN_KEY, min).apply() }

    // ---- Streak & badges ----
    fun getStreak(): Int = prefs.getInt(STREAK_KEY, 0)
    fun saveStreak(v: Int) { prefs.edit().putInt(STREAK_KEY, v).apply() }

    fun getLastActiveDate(): String? = prefs.getString(LAST_ACTIVE_DATE_KEY, null)
    fun saveLastActiveDate(date: String) { prefs.edit().putString(LAST_ACTIVE_DATE_KEY, date).apply() }

    fun getBadges(): MutableList<String> {
        val json = prefs.getString(BADGES_KEY, "[]")
        val type = object : TypeToken<MutableList<String>>() {}.type
        return runCatching { gson.fromJson<MutableList<String>>(json, type) }.getOrElse { mutableListOf() }
    }

    fun saveBadges(list: List<String>) {
        prefs.edit().putString(BADGES_KEY, gson.toJson(list)).apply()
    }

    fun unlockBadge(name: String) {
        val b = getBadges()
        if (!b.contains(name)) { b.add(name); saveBadges(b) }
    }

    // ---- Focus Timer sessions (read-compatible with FocusTimerFragment) ----
    /**dawase focus sessions count (FocusTimerFragment eka record prefs ) */
    fun getFocusSessionsToday(): Int {
        val today = todayStr()
        val lastDay = prefsTimer.getString(FOCUS_DAY_KEY, null)
        return if (lastDay == today) prefsTimer.getInt(FOCUS_COUNT_KEY, 0) else 0
    }

    /** Count eka set/mutate karanna onm menna helpers (optional) */
    fun setFocusSessionsToday(count: Int) {
        val today = todayStr()
        prefsTimer.edit()
            .putString(FOCUS_DAY_KEY, today)
            .putInt(FOCUS_COUNT_KEY, count.coerceAtLeast(0))
            .apply()
    }
    fun bumpFocusSessionsToday() {
        val today = todayStr()
        val lastDay = prefsTimer.getString(FOCUS_DAY_KEY, null)
        var count = if (lastDay == today) prefsTimer.getInt(FOCUS_COUNT_KEY, 0) else 0
        count += 1
        prefsTimer.edit()
            .putString(FOCUS_DAY_KEY, today)
            .putInt(FOCUS_COUNT_KEY, count)
            .apply()
    }

    // ---- Export / Import ----
    data class AppState(
        val quests: List<Quest>,
        val level: Int,
        val xp: Int,
        val moods: List<MoodEntry>,
        val reminderOn: Boolean,
        val reminderMin: Int,
        val streak: Int,
        val lastActiveDate: String?,
        val badges: List<String>
    )

    fun dumpState(): String {
        val state = AppState(
            getAllQuests(),
            getUserLevel(),
            getCurrentXp(),
            getMoodHistory(),
            getReminderStatus(),
            getReminderInterval(),
            getStreak(),
            getLastActiveDate(),
            getBadges()
        )
        return gson.toJson(state)
    }

    fun loadState(json: String) {
        val type = object : TypeToken<AppState>() {}.type
        val s: AppState = gson.fromJson(json, type)
        saveAllQuests(s.quests.toMutableList())
        saveUserLevel(s.level)
        saveCurrentXp(s.xp)
        saveMoodHistory(s.moods.toMutableList())
        setReminderStatus(s.reminderOn)
        setReminderInterval(s.reminderMin)
        saveStreak(s.streak)
        s.lastActiveDate?.let { saveLastActiveDate(it) }
        saveBadges(s.badges)
    }
}

//App state eka SharedPreferences eke JSON lesa save/restore.