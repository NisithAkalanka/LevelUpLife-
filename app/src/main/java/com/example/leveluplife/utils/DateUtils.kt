package com.example.leveluplife.utils
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    fun today(): String = fmt.format(Date())
    fun daysBetween(a: String, b: String): Int {
        val d1 = fmt.parse(a)!!.time; val d2 = fmt.parse(b)!!.time
        return ((d2 - d1) / (1000*60*60*24)).toInt()
    }
}