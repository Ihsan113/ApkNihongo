package com.example.betterseed

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    private val channelName = "betterseed/device_features"
    private val notificationRequest = 9033
    private var pendingResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).setMethodCallHandler { call, result ->
            when (call.method) {
                "getReminder" -> {
                    result.success(mapOf(
                        "enabled" to ReminderScheduler.prefs(this).getBoolean("reminder_enabled", false),
                        "permissionGranted" to notificationGranted()
                    ))
                }
                "setReminder" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    if (enabled && !notificationGranted() && Build.VERSION.SDK_INT >= 33) {
                        if (pendingResult != null) {
                            result.error("REQUEST_IN_PROGRESS", "Permission dialog already open", null)
                        } else {
                            pendingResult = result
                            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationRequest)
                        }
                    } else {
                        ReminderScheduler.setMaster(this, enabled)
                        result.success(mapOf("enabled" to enabled, "permissionGranted" to notificationGranted()))
                    }
                }
                "syncHabitReminders" -> {
                    try {
                        val habits = call.argument<List<Map<String, Any?>>>("habits") ?: emptyList()
                        val array = JSONArray()
                        for (h in habits) {
                            array.put(JSONObject().apply {
                                put("id", h["id"]?.toString() ?: "")
                                put("title", h["title"]?.toString() ?: "")
                                put("completed", h["completed"] == true)
                                put("reminderEnabled", h["reminderEnabled"] == true)
                                put("reminderHour", (h["reminderHour"] as? Number)?.toInt() ?: 20)
                                put("reminderMinute", (h["reminderMinute"] as? Number)?.toInt() ?: 0)
                            })
                        }
                        ReminderScheduler.syncHabits(this, array, call.argument<String>("day") ?: "")
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("REMINDER_SYNC", e.message ?: "Sync failed", null)
                    }
                }
                "updateWidget" -> {
                    val pref = ReminderScheduler.prefs(this)
                    pref.edit()
                        .putInt("widget_water", (call.argument<Int>("water") ?: 0).coerceAtLeast(0))
                        .putInt("widget_stage", (call.argument<Int>("stage") ?: 1).coerceIn(1, 6))
                        .putInt("widget_done", (call.argument<Int>("done") ?: 0).coerceAtLeast(0))
                        .putInt("widget_total", (call.argument<Int>("total") ?: 0).coerceAtLeast(0))
                        .putString("widget_day", call.argument<String>("day") ?: "")
                        .putString("widget_theme", call.argument<String>("theme") ?: "default")
                        .apply()
                    BetterSeedWidget.updateAll(this)
                    result.success(null)
                }
                "pinWidget" -> {
                    if (Build.VERSION.SDK_INT < 26) {
                        result.success(false)
                    } else {
                        val manager = AppWidgetManager.getInstance(this)
                        val component = ComponentName(this, BetterSeedWidget::class.java)
                        result.success(manager.isRequestPinAppWidgetSupported &&
                            manager.requestPinAppWidget(component, null, null))
                    }
                }
                "widgetInstalled" -> {
                    val manager = AppWidgetManager.getInstance(this)
                    result.success(manager.getAppWidgetIds(
                        ComponentName(this, BetterSeedWidget::class.java)).isNotEmpty())
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun notificationGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != notificationRequest) return
        val result = pendingResult ?: return
        pendingResult = null
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ReminderScheduler.setMaster(this, granted)
        result.success(mapOf("enabled" to granted, "permissionGranted" to granted))
    }
}
