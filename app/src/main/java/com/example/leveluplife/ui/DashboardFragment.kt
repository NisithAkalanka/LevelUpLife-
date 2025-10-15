package com.example.leveluplife.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.leveluplife.MainActivity
import com.example.leveluplife.R
import com.example.leveluplife.data.repository.QuestRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

class DashboardFragment : Fragment() {

    private lateinit var repo: QuestRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = QuestRepository(requireContext())

        // Header notch inset
        view.findViewById<View>(R.id.header_container)?.let { header ->
            ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
                val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                v.updatePadding(top = top + v.paddingTop)
                insets
            }
        }

        // Content bottom inset (gesture bar avoid)
        view.findViewById<View>(R.id.scroll)?.let { scroll ->
            ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
                val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                v.updatePadding(bottom = bottom + v.paddingBottom)
                insets
            }
        }

        // Cards
        view.findViewById<MaterialCardView>(R.id.card_quests)
            .setOnClickListener { navigateTo(QuestsFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_mood)
            .setOnClickListener { navigateTo(MoodFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_focus)
            ?.setOnClickListener { navigateTo(FocusTimerFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_settings)
            .setOnClickListener { navigateTo(SettingsFragment()) }

        // Quick actions
        view.findViewById<MaterialButton>(R.id.btn_quick_add_quest)?.setOnClickListener {
            navigateTo(AddQuestFragment())
        }
        view.findViewById<MaterialButton>(R.id.btn_quick_log_mood)?.setOnClickListener {
            navigateTo(MoodFragment())
        }

        // Optional chip shortcuts
        view.findViewById<Chip>(R.id.chip_completed)?.setOnClickListener { navigateTo(QuestsFragment()) }
        view.findViewById<Chip>(R.id.chip_active)?.setOnClickListener { navigateTo(QuestsFragment()) }
        view.findViewById<Chip>(R.id.chip_focus)?.setOnClickListener { navigateTo(FocusTimerFragment()) }

        view.findViewById<TextView>(R.id.tv_quote)?.text = "Grow 1% every day."

        updateUi(view)
        setStatusBarAppearance()
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            updateUi(it)
            setStatusBarAppearance()
        }
    }

    private fun updateUi(root: View) {
        // Level / XP
        val tvLevel    = root.findViewById<TextView>(R.id.tv_user_level)
        val pbXp       = root.findViewById<ProgressBar>(R.id.xp_progress_bar)
        val tvXp       = root.findViewById<TextView>(R.id.tv_xp_progress)
        val chipStreak = root.findViewById<Chip>(R.id.chip_streak)

        val level    = repo.getUserLevel()
        val xp       = repo.getCurrentXp()
        val required = requiredXpForNextLevel(level)

        tvLevel?.text = "LVL $level"
        tvXp?.text = "$xp / $required XP"
        pbXp?.let { bar ->
            bar.max = required
            animateProgress(bar, xp.coerceAtMost(required))
        }
        chipStreak?.text = "Streak: ${repo.getStreak()}d"

        // Today summary chips
        val quests = repo.getAllQuests()
        val completed = quests.count { it.isCompleted }
        val active = quests.size - completed
        val focus = getTodayFocusSessions()

        root.findViewById<Chip>(R.id.chip_completed)?.text = "Completed: $completed"
        root.findViewById<Chip>(R.id.chip_active)?.text    = "Active: $active"
        root.findViewById<Chip>(R.id.chip_focus)?.text     = "Focus: $focus"
    }

    private fun animateProgress(pb: ProgressBar, to: Int) {
        val from = pb.progress
        if (from == to) return
        ObjectAnimator.ofInt(pb, "progress", from, to).apply {
            duration = 350
            start()
        }
    }

    private fun requiredXpForNextLevel(currentLevel: Int): Int {
        val base = 100
        val perLevel = 50
        return base + (currentLevel - 1).coerceAtLeast(0) * perLevel
    }

    // Read today's focus session count (saved by FocusTimerFragment)
    private fun getTodayFocusSessions(): Int {
        val prefs = requireContext().getSharedPreferences("leveluplife_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("focus_sessions_today", 0)
    }

    // Status bar: dark bg + white icons
    private fun setStatusBarAppearance() {
        val window = requireActivity().window
        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.status_bar_dark)
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false
    }

    private fun navigateTo(fragment: Fragment) {
        (activity as? MainActivity)?.navigateTo(fragment)
    }
}