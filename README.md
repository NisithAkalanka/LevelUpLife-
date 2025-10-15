LevelUpLife helps you build better daily habits:
	1.	track & complete Quests,
	2.	quickly log your mood with emoji chips, and
	3.	stay in flow with a Pomodoro-style Focus Timer.

It’s lightweight (no database), offline-first, and uses Material Design for a modern feel.


✨ Features
	•	Quests
	•	Add/search quests, mark complete, swipe-to-delete with Snackbar Undo
	•	XP & Level progression shown on Dashboard
	•	Mood Journal
	•	One-tap emoji chips + optional note & tags
	•	Recent mood history (RecyclerView)
	•	Share latest mood via implicit intent
	•	Focus Timer
	•	25/15/5 presets, start/pause/resume/reset
	•	Animated CircularProgressIndicator, keep-screen-on, vibration on finish
	•	“Sessions today” count saved locally
	•	Hydration Reminder
	•	Toggle + interval setting
	•	AlarmManager → BroadcastReceiver → notification (Android 13+ permission handled)
	•	Gamification
	•	XP, Level bar, daily streak chip, milestone badges (placeholders ready)
	•	UI/UX
	•	Collapsing gradient header, haptics on chip taps, empty states, undo for destructive actions


🧱 Architecture & Tech
	•	Navigation: Single-activity, multi-fragment with explicit fragment navigation
	•	Pattern: MVVM — QuestsViewModel + QuestRepository manage state
	•	Data: SharedPreferences + Gson (lists stored as JSON; no Room/DB)
	•	UI: Material 3 components, RecyclerView + ListAdapter/DiffUtil
	•	Offline-first: No network; all data stays on device


app/
 └─ src/main/java/com/example/leveluplife/
    ├─ MainActivity.kt
    ├─ ui/
    │   ├─ DashboardFragment.kt
    │   ├─ QuestsFragment.kt
    │   ├─ MoodFragment.kt
    │   ├─ FocusTimerFragment.kt
    │   ├─ SettingsFragment.kt
    │   ├─ QuestsAdapter.kt
    │   ├─ MoodHistoryAdapter.kt
    │   └─ NotificationReceiver.kt
    ├─ data/
    │   ├─ Quest.kt
    │   ├─ MoodEntry.kt
    │   └─ repository/QuestRepository.kt
    └─ ui/QuestsViewModel.kt



    🤝 Contributing

PRs are welcome. Please keep UI consistent with Material 3 and follow MVVM + repository pattern.
