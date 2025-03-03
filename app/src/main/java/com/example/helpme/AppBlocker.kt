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
    
    // Track the last detected foreground app for better reliability
    private var lastKnownForegroundApp: String? = null
    private var lastForegroundAppTimestamp: Long = 0

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
            // Use a larger time window (60 seconds) to catch more events
            val usageEvents = usageStatsManager.queryEvents(time - 60000, time)
            var lastForegroundEvent: UsageEvents.Event? = null
            var lastHomeEvent: UsageEvents.Event? = null
            var lastNonHelpMeApp: UsageEvents.Event? = null

            // Track both foreground and home events
            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)

                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        // Update last foreground event if it's not a launcher
                        if (!event.packageName.startsWith("com.android.launcher")) {
                            lastForegroundEvent = event
                            
                            // Keep track of the last non-HelpMe app for better fallback
                            if (event.packageName != "com.example.helpme") {
                                lastNonHelpMeApp = event
                                
                                // Update our cache of the last known foreground app
                                lastKnownForegroundApp = event.packageName
                                lastForegroundAppTimestamp = event.timeStamp
                                
                                Log.d("AppBlocker", "Cached foreground app: ${event.packageName}")
                            }
                        } else {
                            lastHomeEvent = event
                        }
                    }
                }
            }

            // If we have a recent foreground event, use it
            if (lastForegroundEvent != null) {
                val timeSinceEvent = time - lastForegroundEvent.timeStamp
                if (timeSinceEvent <= 60000) { // Within last 60 seconds
                    return lastForegroundEvent.packageName
                }
            }

            // If we recently went to home screen, return the last non-HelpMe app we saw
            if (lastHomeEvent != null) {
                val timeSinceHome = time - lastHomeEvent.timeStamp
                if (timeSinceHome <= 10000) { // Within last 10 seconds
                    // Return the last non-HelpMe app we saw before the home event
                    if (lastNonHelpMeApp != null && 
                        lastNonHelpMeApp.timeStamp < lastHomeEvent.timeStamp && 
                        (lastHomeEvent.timeStamp - lastNonHelpMeApp.timeStamp) < 15000) {
                        return lastNonHelpMeApp.packageName
                    }
                }
            }

            // If we have any foreground event, return it as last resort
            return lastForegroundEvent?.packageName
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
        // First try to get the current app
        val currentApp = getCurrentForegroundApp(context)
        Log.d("AppBlocker", "Attempting to block current app. Detected: $currentApp")

        // If successful and it's not our own app or launcher
        if (currentApp != null &&
            currentApp != "com.example.helpme" &&
            !currentApp.startsWith("com.android.launcher")) {

            // Block specific app
            blockApp(context, currentApp)
        } else {
            // Try using our cached last known app
            val timeSinceLastKnown = System.currentTimeMillis() - lastForegroundAppTimestamp
            
            if (lastKnownForegroundApp != null && 
                lastKnownForegroundApp != "com.example.helpme" &&
                !lastKnownForegroundApp!!.startsWith("com.android.launcher") &&
                timeSinceLastKnown < 60000) { // Only use cache from last 60 seconds
                
                // Use our cached last known app
                Log.d("AppBlocker", "Using cached app for blocking: $lastKnownForegroundApp")
                blockApp(context, lastKnownForegroundApp!!)
            } else {
                // Ultimate fallback - block "unknown" app
                Log.d("AppBlocker", "Using fallback blocking - no app could be determined")

                // Set a generic block time
                val unblockTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(DEFAULT_BLOCK_DURATION_MINUTES)

                // Let BlockOverlayManager handle returning to home screen
                BlockOverlayManager.showBlockingOverlay(context, "unknown", unblockTime)
            }
        }
    }
}