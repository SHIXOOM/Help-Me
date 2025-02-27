package com.example.helpme

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import java.util.concurrent.TimeUnit

// Only store data, not UI elements
object AppBlocker {
    private val blockedApps = mutableMapOf<String, Long>() // Package name -> Unblock time
    const val DEFAULT_BLOCK_DURATION_MINUTES = 10L

    fun blockApp(context: Context, packageName: String) {
        val unblockTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(
            DEFAULT_BLOCK_DURATION_MINUTES
        )
        blockedApps[packageName] = unblockTime

        Log.d("AppBlocker", "Blocked app: $packageName for $DEFAULT_BLOCK_DURATION_MINUTES minutes")

        // Create a blocking overlay immediately if this app is in foreground
        val currentApp = getCurrentForegroundApp(context)
        if (currentApp == packageName) {
            // Let BlockOverlayManager handle returning to home screen
            BlockOverlayManager.showBlockingOverlay(context, packageName, unblockTime)
        }
    }

    fun getCurrentForegroundApp(context: Context): String? {
        if (!hasUsageStatsPermission(context)) {
            return null
        }

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        try {
            // Get usage events for the last 10 seconds
            val usageEvents = usageStatsManager.queryEvents(time - 10000, time)
            var lastEvent: UsageEvents.Event? = null

            // Find the last foreground event
            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)

                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastEvent = event
                }
            }

            return lastEvent?.packageName
        } catch (e: Exception) {
            Log.e("AppBlocker", "Error getting foreground app", e)
            return null
        }
    }

    fun isAppBlocked(packageName: String): Boolean {
        val unblockTime = blockedApps[packageName] ?: return false
        return System.currentTimeMillis() < unblockTime
    }

    fun getUnblockTime(packageName: String): Long {
        return blockedApps[packageName] ?: 0
    }

    fun checkAndBlockIfNeeded(context: Context): Boolean {
        val currentApp = getCurrentForegroundApp(context)

        if (currentApp != null && isAppBlocked(currentApp) &&
            currentApp != "com.example.helpme" &&
            !currentApp.startsWith("com.android.launcher")
        ) {
            BlockOverlayManager.showBlockingOverlay(
                context,
                currentApp,
                getUnblockTime(currentApp)
            )
            return true
        }
        return false
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun blockCurrentAppWithFallback(context: Context) {
        val currentApp = getCurrentForegroundApp(context)
        Log.d("AppBlocker", "Attempting to block current app. Detected: $currentApp")

        if (currentApp != null &&
            currentApp != "com.example.helpme" &&
            !currentApp.startsWith("com.android.launcher")) {

            // Block specific app
            blockApp(context, currentApp)
        } else {
            // Fallback - block "unknown" app
            Log.d("AppBlocker", "Using fallback blocking")

            // Set a generic block time
            val unblockTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(DEFAULT_BLOCK_DURATION_MINUTES)

            // Let BlockOverlayManager handle returning to home screen
            BlockOverlayManager.showBlockingOverlay(context, "unknown", unblockTime)
        }
    }
}