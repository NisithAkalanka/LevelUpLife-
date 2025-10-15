// app/src/main/java/com/example/leveluplife/ui/QuestsViewModel.kt
package com.example.leveluplife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.leveluplife.data.MoodEntry
import com.example.leveluplife.data.Quest
import com.example.leveluplife.data.repository.QuestRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class QuestsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = QuestRepository(app.applicationContext)

    // UI caches
    val questList: MutableList<Quest> = mutableListOf()
    val moodHistory: MutableList<MoodEntry> = mutableListOf()

    companion object {
        const val XP_FOR_LOGGING_MOOD = 10
    }

    init { load() }

    /** Repo → UI cache refresh (Quests + Moods) */
    fun load() {
        questList.clear()
        questList.addAll(repo.getAllQuests())

        moodHistory.clear()
        moodHistory.addAll(repo.getMoodHistory().sortedByDescending { it.timestamp })
    }

    /** ---------- Quests ---------- */
    fun addNewQuest(title: String) {
        val newQ = Quest(title = title)
        val newList = questList.toMutableList().apply { add(0, newQ) }
        repo.saveAllQuests(newList)
        questList.clear(); questList.addAll(newList)
    }

    fun toggleQuest(id: String) {
        val newList = questList.map { q ->
            if (q.id == id) q.copy(isCompleted = !q.isCompleted) else q
        }
        repo.saveAllQuests(newList.toMutableList())
        questList.clear(); questList.addAll(newList)
    }

    fun deleteQuest(id: String) {
        val newList = questList.filterNot { it.id == id }
        repo.saveAllQuests(newList.toMutableList())
        questList.clear(); questList.addAll(newList)
    }

    fun renameQuest(id: String, newTitle: String) {
        val newList = questList.map { q ->
            if (q.id == id) q.copy(title = newTitle) else q
        }
        repo.saveAllQuests(newList.toMutableList())
        questList.clear(); questList.addAll(newList)
    }

    fun reorderQuests(newOrder: List<Quest>) {
        // newOrder එක UI අතේ තිබ්බ order 그대로 save කරන්න
        repo.saveAllQuests(newOrder.toMutableList())
        questList.clear(); questList.addAll(newOrder)
    }

    /** ---------- Moods ---------- */
    fun logNewMood(mood: String, emoji: String, note: String?, tags: List<String>) {
        val entry = MoodEntry(
            id = UUID.randomUUID().toString(),
            emoji = emoji,
            mood = mood,
            note = note,
            tags = tags,
            timestamp = System.currentTimeMillis()
        )
        val newList = repo.getMoodHistory().toMutableList().apply { add(0, entry) }
        repo.saveMoodHistory(newList)
        moodHistory.clear(); moodHistory.addAll(newList)
        grantXp(XP_FOR_LOGGING_MOOD)
    }

    fun logFromChipLabel(label: String, note: String?, tags: List<String>) {
        val parts = label.trim().split(" ")
        val emoji = parts.firstOrNull() ?: "🙂"
        val mood = parts.getOrNull(1) ?: "Neutral"
        logNewMood(mood, emoji, note, tags)
    }

    fun undoLastMood(adjustXp: Boolean = false): Boolean {
        val list = repo.getMoodHistory().toMutableList()
        if (list.isEmpty()) return false
        list.removeAt(0)
        repo.saveMoodHistory(list)
        moodHistory.clear(); moodHistory.addAll(list)
        if (adjustXp) grantXp(-XP_FOR_LOGGING_MOOD)
        return true
    }

    fun clearAllMoods() {
        repo.saveMoodHistory(emptyList())
        moodHistory.clear()
    }

    fun suggestQuestForMood(mood: String) {
        val title = when (mood) {
            "Sad"      -> "Box Breathing – 3 minutes"
            "Stressed" -> "5-minute body scan"
            "Neutral"  -> "Gratitude note – 1 line"
            "Calm"     -> "Short walk – 10 mins"
            "Excited"  -> "Plan tomorrow – 3 bullets"
            else       -> return
        }
        if (questList.none { it.title == title }) addNewQuest(title)
    }

    /** ---------- XP / Level ---------- */
    private fun grantXp(delta: Int) {
        var xp = repo.getCurrentXp() + delta
        var level = repo.getUserLevel()
        var need = requiredXpForNextLevel(level)

        while (xp >= need) {
            xp -= need
            level += 1
            need = requiredXpForNextLevel(level)
        }
        if (xp < 0) xp = 0
        if (level < 1) level = 1

        repo.saveUserLevel(level)
        repo.saveCurrentXp(xp)
    }

    fun requiredXpForNextLevel(level: Int): Int {
        val base = 100
        val perLevel = 50
        return base + (level - 1).coerceAtLeast(0) * perLevel
    }

    /** ---------- Streak / Badges ---------- */
    fun updateStreakAndBadges() {
        val total = questList.size
        val completed = questList.count { it.isCompleted }
        val success = total > 0 && (completed * 100 / total) >= 60

        val today = todayStr()
        val last = runCatching { repo.getLastActiveDate() }.getOrNull()
        var streak = runCatching { repo.getStreak() }.getOrElse { 0 }

        if (last != today) {
            streak = if (success) streak + 1 else 0
            runCatching { repo.saveStreak(streak) }
            runCatching { repo.saveLastActiveDate(today) }

            if (streak >= 3)  runCatching { repo.unlockBadge("🔥 3-day streak") }
            if (streak >= 7)  runCatching { repo.unlockBadge("🏅 7-day streak") }
            if (streak >= 14) runCatching { repo.unlockBadge("🧠 14-day streak") }
            if (streak >= 30) runCatching { repo.unlockBadge("👑 30-day streak") }
        }
    }

    /** ---------- Helpers ---------- */
    fun todayCount(): Int {
        val start = startOfDayMillis()
        return moodHistory.count { it.timestamp >= start }
    }

    fun last7DaysStats(): Map<String, Int> {
        val sevenAgo = System.currentTimeMillis() - 6 * 24 * 60 * 60 * 1000L
        return moodHistory
            .filter { it.timestamp >= startOfDayMillis(sevenAgo) }
            .groupingBy { it.mood }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .toMap()
    }

    private fun todayStr(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(System.currentTimeMillis())
    }

    private fun startOfDayMillis(base: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = base
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }
}