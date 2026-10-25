package com.example.betterseed

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Read-only, battery-friendly widget. Stage art is bundled locally. */
class BetterSeedWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager,
                                           appWidgetId: Int, newOptions: android.os.Bundle) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        updateOne(context, manager, appWidgetId)
    }

    companion object {
        private val treeArt = intArrayOf(
            R.drawable.widget_tree_0, R.drawable.widget_tree_1,
            R.drawable.widget_tree_2, R.drawable.widget_tree_3,
            R.drawable.widget_tree_4, R.drawable.widget_tree_5
        )

        private val moonTreeArt = intArrayOf(
            R.drawable.widget_moon_tree_0, R.drawable.widget_moon_tree_1,
            R.drawable.widget_moon_tree_2, R.drawable.widget_moon_tree_3,
            R.drawable.widget_moon_tree_4, R.drawable.widget_moon_tree_5
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BetterSeedWidget::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val pref = ReminderScheduler.prefs(context)
            val drops = pref.getInt("widget_water", 0).coerceAtLeast(0)
            val stage = pref.getInt("widget_stage", 1).coerceIn(1, 6)
            val currentDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val done = if (pref.getString("widget_day", "") == currentDay) {
                pref.getInt("widget_done", 0).coerceAtLeast(0)
            } else 0
            val total = pref.getInt("widget_total", 0).coerceAtLeast(0)
            val views = RemoteViews(context.packageName, R.layout.betterseed_widget)
            views.setTextViewText(R.id.widget_water, "💧 $drops water")
            views.setTextViewText(R.id.widget_stage, "Stage $stage / 6")
            views.setTextViewText(R.id.widget_habits, "$done / $total habits done")
            val moonlight = pref.getString("widget_theme", "default") == "moonlight"
            views.setImageViewResource(R.id.widget_tree_art,
                if (moonlight) moonTreeArt[stage - 1] else treeArt[stage - 1])
            views.setProgressBar(R.id.widget_habit_progress, 100,
                if (total == 0) 0 else (100 * done / total).toInt().coerceIn(0, 100), false)
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launch != null) {
                views.setOnClickPendingIntent(R.id.widget_root,
                    PendingIntent.getActivity(context, 9032, launch,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            manager.updateAppWidget(id, views)
        }
    }
}
