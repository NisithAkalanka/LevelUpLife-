package com.example.leveluplife.data

import java.util.Calendar
import java.util.UUID

data class Quest(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var isCompleted: Boolean = false,
    var targetPerDay: Int = 1
)
object Quotes {
    private val list = listOf(
        "Small steps, big gains.",
        "Discipline beats motivation.",
        "Show up today. Future-you will thank you.",
        "Done is better than perfect.",
        "Grow 1% every day.",
        "What gets measured, improves.",
        "Focus. One thing at a time.",
        "Energy flows where attention goes.",
        "Consistency compounds.",
        "Be patient with the process."
    )
    fun today(): String {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return list[day % list.size]
    }
}

//Daily tasks list UI