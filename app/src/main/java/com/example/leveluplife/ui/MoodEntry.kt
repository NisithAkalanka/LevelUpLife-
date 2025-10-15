package com.example.leveluplife.data
data class MoodEntry(
    val id: String,
    val emoji: String,
    val mood: String,
    val note: String?,
    val tags: List<String> = emptyList(),
    val timestamp: Long
)

//Mood journal history, stats, XP grant