package com.example.leveluplife.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.leveluplife.R
import com.example.leveluplife.data.MoodEntry
import java.text.SimpleDateFormat
import java.util.*

class MoodHistoryAdapter(val items: MutableList<MoodEntry>) :
    RecyclerView.Adapter<MoodHistoryAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvEmoji: TextView = v.findViewById(R.id.tvEmoji)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(p.context).inflate(R.layout.mood_item, p, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = items[pos]
        h.tvEmoji.text = m.emoji
        val fmt = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        h.tvDate.text = fmt.format(Date(m.timestamp))
    }
}