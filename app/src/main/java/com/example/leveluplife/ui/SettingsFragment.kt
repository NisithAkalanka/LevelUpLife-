package com.example.leveluplife.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.leveluplife.R
import com.example.leveluplife.data.repository.QuestRepository
import com.example.leveluplife.util.NotificationReceiver
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    // Data
    private val repository by lazy { QuestRepository(requireContext()) }

    // Alarm infra
    private lateinit var alarmManager: AlarmManager
    private var pendingIntent: PendingIntent? = null

    // Android 13+ runtime notif permission
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val minutes = repository.getReminderInterval()
            setAlarm(minutes)
            Toast.makeText(context, "Notifications enabled. Reminder ON.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createNotificationChannel()

        val toolbar: Toolbar = view.findViewById(R.id.toolbar_settings)
        val reminderSwitch: SwitchMaterial = view.findViewById(R.id.switch_hydration_reminder)
        val intervalEditText: TextInputEditText = view.findViewById(R.id.et_reminder_interval)
        val saveButton: Button = view.findViewById(R.id.btn_save_settings)

        toolbar.setNavigationOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        // Load saved state
        reminderSwitch.isChecked = repository.getReminderStatus()
        intervalEditText.setText(repository.getReminderInterval().toString())

        saveButton.setOnClickListener {
            val isEnabled = reminderSwitch.isChecked
            val minutes = intervalEditText.text?.toString()?.trim()?.toIntOrNull()

            if (minutes == null || minutes <= 0) {
                Toast.makeText(context, "Please enter a valid number of minutes.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            repository.setReminderStatus(isEnabled)
            repository.setReminderInterval(minutes)

            if (isEnabled) {
                // Android 13+ permission
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnClickListener
                }
                setAlarm(minutes)
                Toast.makeText(context, "Hydration Reminder ON. Every $minutes minutes.", Toast.LENGTH_LONG).show()
            } else {
                cancelAlarm()
                Toast.makeText(context, "Hydration Reminder OFF.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Repeating reminder — battery-friendly inexact repeating */
    private fun setAlarm(intervalMinutes: Int) {
        alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java)

        // Stable PendingIntent signature
        val pi: PendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            REQ_HYDRATION,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        pendingIntent = pi

        val intervalMs = intervalMinutes * 60_000L
        val firstTrigger = System.currentTimeMillis() + intervalMs

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            firstTrigger,
            intervalMs,
            pi
        )

        // Show next trigger time
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(firstTrigger))
        Toast.makeText(requireContext(), "Next reminder at $time", Toast.LENGTH_LONG).show()
    }

    private fun cancelAlarm() {
        alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java)

        val pi: PendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            REQ_HYDRATION,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pi)
        pendingIntent = null
    }

    /** Notification channel for hydration reminders (must match receiver) */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hydration Reminders"
            val descriptionText = "Channel for water drinking reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val nm = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val REQ_HYDRATION = 1001
        const val CHANNEL_ID = "hydration_channel_id" // NotificationReceiver දාන්නම මේක
    }
}