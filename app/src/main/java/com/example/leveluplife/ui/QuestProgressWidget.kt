package com.example.leveluplife.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.leveluplife.MainActivity
import com.example.leveluplife.R
import com.example.leveluplife.data.repository.QuestRepository

class QuestProgressWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        //
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val repository = QuestRepository(context)
        val quests = repository.getAllQuests()

        val completedCount = quests.count { it.isCompleted }
        val totalCount = quests.size
        val progressPercentage = if (totalCount > 0) {
            (completedCount * 100 / totalCount)
        } else 0

        val views = RemoteViews(context.packageName, R.layout.quest_widget_layout).apply {
            // Text + ProgressBar update
            setTextViewText(R.id.widget_progress_text, "$completedCount/$totalCount Quests")
            setProgressBar(R.id.widget_progress_bar, 100, progressPercentage, false)

            // Click → open app (Quests screen)
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("open", "quests")
            }
            val pi = PendingIntent.getActivity(
                context,
                REQ_OPEN_APP,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            setOnClickPendingIntent(R.id.widget_container, pi)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        private const val REQ_OPEN_APP = 2001
    }
}