package com.example.leveluplife.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.leveluplife.R

class SuggestionsAdapter(
    private val suggestions: List<String>,
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder>() {

    inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        // අපි මේකට Android system එකේම තියෙන සරල list item layout එකක් පාවිච්චි කරමු.
        // අලුතෙන් XML හදන්න ඕන නෑ!
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.textView.text = suggestion
        holder.itemView.setOnClickListener {
            onItemClicked(suggestion)
        }
    }

    override fun getItemCount(): Int = suggestions.size
}
