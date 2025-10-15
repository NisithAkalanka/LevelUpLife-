package com.example.leveluplife.utils

import java.util.Calendar

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