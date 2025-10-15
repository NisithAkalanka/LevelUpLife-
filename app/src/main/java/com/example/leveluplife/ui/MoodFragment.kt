package com.example.leveluplife.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leveluplife.R
import com.example.leveluplife.data.MoodEntry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar

class MoodFragment : Fragment() {

    private val vm: QuestsViewModel by activityViewModels()
    private lateinit var adapter: MoodHistoryAdapter
    private lateinit var rv: RecyclerView

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_mood, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        // Toolbar
        v.findViewById<MaterialToolbar>(R.id.toolbar_mood).apply {
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            setOnMenuItemClickListener { it.itemId == R.id.action_share && run { shareLatest(); true } }
        }

        // Recycler
        rv = v.findViewById(R.id.mood_history_recycler_view)
        adapter = MoodHistoryAdapter(vm.moodHistory)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        toggleEmpty(v)

        // Inputs
        val chipsEmojis = v.findViewById<ChipGroup>(R.id.chips_emojis)
        val chipsTags   = v.findViewById<ChipGroup>(R.id.chipgroup_tags)
        val etNote      = v.findViewById<TextInputEditText>(R.id.et_mood_note)
        val btnLog      = v.findViewById<MaterialButton>(R.id.btn_log_mood)
        val btnClear    = v.findViewById<MaterialButton>(R.id.btn_clear_note)

        // Small engagement: scale animation + haptic on select
        fun animateChip(chip: Chip) {
            chip.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            chip.animate().cancel()
            chip.scaleX = 0.9f; chip.scaleY = 0.9f
            chip.animate().scaleX(1f).scaleY(1f)
                .setDuration(160).setInterpolator(OvershootInterpolator()).start()
        }
        for (i in 0 until chipsEmojis.childCount) {
            (chipsEmojis.getChildAt(i) as? Chip)?.setOnCheckedChangeListener { c, checked ->
                if (checked) animateChip(c as Chip)
            }
        }

        // Log mood
        btnLog.setOnClickListener {
            val id = chipsEmojis.checkedChipId
            if (id == View.NO_ID) {
                Toast.makeText(requireContext(), "Select an emoji first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selected = v.findViewById<Chip>(id)
            val text = selected.text.toString()           // e.g. "😊 Happy"
            val emoji = text.split(" ").firstOrNull() ?: "🙂"
            val mood  = (selected.tag as? String) ?: "Neutral"
            val note  = etNote.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
            val tags  = chipsTags.checkedChipIds.mapNotNull { v.findViewById<Chip>(it)?.text?.toString() }

            vm.logNewMood(mood, emoji, note, tags)
            adapter.notifyItemInserted(0)
            rv.smoothScrollToPosition(0)
            toggleEmpty(v)

            // ✅ Class name හරහා constant access + proper XP undo
            Snackbar.make(v, "Logged $mood • +${QuestsViewModel.XP_FOR_LOGGING_MOOD} XP", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    if (vm.undoLastMood(adjustXp = true)) {
                        adapter.notifyDataSetChanged()
                        toggleEmpty(v)
                    }
                }.show()

            // reset inputs
            etNote.setText("")
            chipsEmojis.clearCheck()
            chipsTags.clearCheck()
        }

        // Clear (short press = inputs clear)
        btnClear.setOnClickListener {
            etNote.setText("")
            chipsEmojis.clearCheck()
            chipsTags.clearCheck()
            Toast.makeText(requireContext(), "Cleared selection.", Toast.LENGTH_SHORT).show()
        }

        // Clear (long press = clear history with confirm)
        btnClear.setOnLongClickListener {
            val backup = vm.moodHistory.toList() // for undo
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear recent moods?")
                .setMessage("This will remove all mood entries.")
                .setPositiveButton("Clear") { _, _ ->
                    vm.clearAllMoods()
                    adapter.notifyDataSetChanged()
                    toggleEmpty(v)
                    Snackbar.make(v, "History cleared", Snackbar.LENGTH_LONG)
                        .setAction("Undo") {
                            vm.clearAllMoods()
                            backup.forEach { e -> vm.logNewMood(e.mood, e.emoji, e.note, e.tags) }
                            adapter.notifyDataSetChanged()
                            toggleEmpty(v)
                        }.show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }
    }

    private fun toggleEmpty(root: View) {
        val empty = root.findViewById<View>(R.id.tv_empty_moods)
        empty?.visibility = if (vm.moodHistory.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun shareLatest() {
        val latest = vm.moodHistory.firstOrNull() ?: run {
            Toast.makeText(requireContext(), "Log a mood first to share!", Toast.LENGTH_SHORT).show(); return
        }
        val msg = "Feeling ${latest.mood} ${latest.emoji} today on my LevelUp Life journey! #LevelUpLife"
        val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, msg) }
        try {
            startActivity(Intent.createChooser(i, "Share your mood"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No app found to share.", Toast.LENGTH_SHORT).show()
        }
    }
}