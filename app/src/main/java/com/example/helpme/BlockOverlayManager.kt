package com.example.helpme

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference

/**
 * Non-singleton class to manage app blocking without visual overlays
 */
object BlockOverlayManager {
    // Keep a weak reference to the current controller (if any)
    private var currentController: WeakReference<BlockOverlayController>? = null

    fun showBlockingOverlay(context: Context, packageName: String, unblockTime: Long) {
        // Dismiss any existing controller
        currentController?.get()?.dismiss()

        // Create a new controller with this context
        val controller = BlockOverlayController(context)
        currentController = WeakReference(controller)

        // Handle the blocking (no visual overlay, just go home)
        controller.handleBlocking(packageName, unblockTime)
    }

    /**
     * Non-static controller class to manage app blocking
     */
    private class BlockOverlayController(context: Context) {
        private val contextRef = WeakReference(context)
        private val handler = Handler(Looper.getMainLooper())
        private var dismissTask: Runnable? = null

        fun handleBlocking(packageName: String, unblockTime: Long) {
            val context = contextRef.get() ?: return
            
            Log.d("BlockOverlayManager", "Blocking app: $packageName until ${unblockTime}")
            
            // Simply navigate to home screen
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(homeIntent)
            
            // No need for auto-dismiss since we're not showing an overlay
            // But we'll keep the controller alive for a short time to prevent immediate recreation
            dismissTask = Runnable { dismiss() }
            handler.postDelayed(dismissTask!!, 1000) // Just keep controller alive briefly
        }

        fun dismiss() {
            // Remove any pending dismiss callbacks
            dismissTask?.let { handler.removeCallbacks(it) }
            dismissTask = null
        }
    }
}