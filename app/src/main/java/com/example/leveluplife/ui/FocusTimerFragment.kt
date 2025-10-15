package com.example.leveluplife.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.leveluplife.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

class FocusTimerFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvCountdown: TextView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var btn25: MaterialButton
    private lateinit var btn15: MaterialButton
    private lateinit var btn5: MaterialButton

    private var totalMillis: Long = 25 * 60_000L  // default 25m
    private var remainingMillis: Long = totalMillis
    private var running = false
    private var timer: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_focus_timer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar        = view.findViewById(R.id.toolbar_focus)
        tvCountdown    = view.findViewById(R.id.tvCountdown)
        progress       = view.findViewById(R.id.progress_circular)
        btnStartPause  = view.findViewById(R.id.btnStartPause)
        btnReset       = view.findViewById(R.id.btnReset)
        btn25          = view.findViewById(R.id.btn25)
        btn15          = view.findViewById(R.id.btn15)
        btn5           = view.findViewById(R.id.btn5)

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // restore state (rotation etc.)
        if (savedInstanceState != null) {
            totalMillis     = savedInstanceState.getLong("total", totalMillis)
            remainingMillis = savedInstanceState.getLong("remain", totalMillis)
            running         = savedInstanceState.getBoolean("running", false)
        }

        updateUi()

        btn25.setOnClickListener { setDurationMinutes(25) }
        btn15.setOnClickListener { setDurationMinutes(15) }
        btn5.setOnClickListener  { setDurationMinutes(5)  }

        btnStartPause.setOnClickListener { toggleStartPause() }
        btnReset.setOnClickListener { resetTimer() }

        if (running) startInternal() // resume after rotate
        setRunningUi(running)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("total", totalMillis)
        outState.putLong("remain", remainingMillis)
        outState.putBoolean("running", running)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        // background service නැති නිසා pause කරලා UI sync කරන්න
        if (running) pauseInternal()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        clearKeepScreenOn()
    }

    private fun setDurationMinutes(m: Int) {
        pauseInternal()
        totalMillis = m * 60_000L
        remainingMillis = totalMillis
        btnStartPause.text = getString(R.string.start) // "Start"
        updateUi()
        setRunningUi(false)
    }

    private fun toggleStartPause() {
        if (running) {
            pauseInternal()
        } else {
            startInternal()
        }
    }

    private fun startInternal() {
        running = true
        btnStartPause.text = getString(R.string.pause) // "Pause"
        setRunningUi(true)

        timer?.cancel()
        val startAt = max(remainingMillis, 0L)
        timer = object : CountDownTimer(startAt, 1000L) {
            override fun onTick(ms: Long) {
                remainingMillis = ms
                updateUi()
            }
            override fun onFinish() {
                remainingMillis = 0L
                running = false
                btnStartPause.text = getString(R.string.start)
                updateUi()
                setRunningUi(false)
                celebrateFinish()
                recordFocusSession() // <-- 🔔 Dashboard chips එකට ගණන update වෙන්නේ මේකෙන්
            }
        }.start()
    }

    private fun pauseInternal() {
        running = false
        timer?.cancel()
        btnStartPause.text = getString(R.string.resume) // "Resume"
        setRunningUi(false)
    }

    private fun resetTimer() {
        pauseInternal()
        remainingMillis = totalMillis
        btnStartPause.text = getString(R.string.start)
        updateUi()
    }

    private fun updateUi() {
        val totalSeconds = (remainingMillis / 1000).toInt()
        val mm = totalSeconds / 60
        val ss = totalSeconds % 60
        tvCountdown.text = String.format("%02d:%02d", mm, ss)

        // remaining% → ring progress (0..100)
        val pct = if (totalMillis == 0L) 0 else ((remainingMillis.toDouble() / totalMillis) * 100).toInt()
        progress.setProgressCompat(pct.coerceIn(0, 100), /*animate=*/true)
    }

    private fun setRunningUi(isRunning: Boolean) {
        // Presets lock while running
        btn25.isEnabled = !isRunning
        btn15.isEnabled = !isRunning
        btn5.isEnabled  = !isRunning

        if (isRunning) keepScreenOn() else clearKeepScreenOn()
    }

    private fun keepScreenOn() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun clearKeepScreenOn() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** අවසානයේ animation + vibrate + Toast */
    private fun celebrateFinish() {
        // pulse animation on the time text
        val upX = ObjectAnimator.ofFloat(tvCountdown, View.SCALE_X, 1f, 1.25f)
        val upY = ObjectAnimator.ofFloat(tvCountdown, View.SCALE_Y, 1f, 1.25f)
        val downX = ObjectAnimator.ofFloat(tvCountdown, View.SCALE_X, 1.25f, 1f)
        val downY = ObjectAnimator.ofFloat(tvCountdown, View.SCALE_Y, 1.25f, 1f)
        AnimatorSet().apply {
            duration = 650
            playTogether(upX, upY)
            start()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    AnimatorSet().apply {
                        duration = 350
                        playTogether(downX, downY)
                        start()
                    }
                }
            })
        }

        // small vibrate
        try {
            val vib = if (android.os.Build.VERSION.SDK_INT >= 31) {
                val vm = requireContext().getSystemService(VibratorManager::class.java)
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Vibrator::class.java)
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vib?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(300)
            }
        } catch (_: Throwable) {}

        Toast.makeText(requireContext(), "Time’s up! 🎯", Toast.LENGTH_SHORT).show()
    }

    /** 🎯 Today focus session count එක prefs වලට ලියන එක */
    private fun recordFocusSession() {
        val prefs = requireContext().getSharedPreferences("leveluplife_prefs", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
        val lastDay = prefs.getString("focus_sessions_day", null)
        var count = prefs.getInt("focus_sessions_today", 0)
        if (lastDay != today) count = 0
        count += 1
        prefs.edit()
            .putString("focus_sessions_day", today)
            .putInt("focus_sessions_today", count)
            .apply()
    }
}