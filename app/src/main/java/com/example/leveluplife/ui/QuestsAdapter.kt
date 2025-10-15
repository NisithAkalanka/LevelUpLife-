package com.example.leveluplife.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.leveluplife.R
import com.example.leveluplife.data.Quest
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox

class QuestsAdapter(
    private val onChecked: (Quest, Boolean) -> Unit, // checkbox toggle
    private val onDelete:  (Quest) -> Unit,           // trash button
    private val onEdit:    (Quest) -> Unit            // card click -> rename dialog
) : ListAdapter<Quest, QuestsAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Quest>() {
            override fun areItemsTheSame(old: Quest, new: Quest) = old.id == new.id
            override fun areContentsTheSame(old: Quest, new: Quest) =
                old.title == new.title && old.isCompleted == new.isCompleted
        }
    }

    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.quest_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), onChecked, onDelete, onEdit)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView // root is card
        private val cb: MaterialCheckBox   = itemView.findViewById(R.id.cbDone)
        private val tvTitle: TextView      = itemView.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView   = itemView.findViewById(R.id.tvSubtitle)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(
            q: Quest,
            onChecked: (Quest, Boolean) -> Unit,
            onDelete:  (Quest) -> Unit,
            onEdit:    (Quest) -> Unit
        ) {
            tvTitle.text = q.title
            tvSubtitle.setText(
                if (q.isCompleted) R.string.completed else R.string.tap_to_mark_complete
            )

            // avoid double-callback when recycling
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = q.isCompleted
            cb.setOnCheckedChangeListener { _, checked -> onChecked(q, checked) }

            btnDelete.setOnClickListener { onDelete(q) }
            card.setOnClickListener { onEdit(q) }
        }
    }
}