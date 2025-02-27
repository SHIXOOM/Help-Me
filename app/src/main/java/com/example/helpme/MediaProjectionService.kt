package com.example.helpme

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import android.app.Activity

import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MediaProjectionService : Service() {

    companion object {
        const val CHANNEL_ID = "MediaProjectionServiceChannel"
        const val NOTIFICATION_ID = 1
        const val CAPTURE_DELAY = 10000L // 10 seconds in milliseconds
    }

    private lateinit var handler: Handler
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            handler.removeCallbacks(captureRunnable)
            mediaProjection = null
            virtualDisplay?.release()
            imageReader?.close()
        }
    }

    // Not strictly needed unless you wish to monitor virtual display state.
    private val virtualDisplayCallback = object : VirtualDisplay.Callback() {
        override fun onStopped() {
            Log.d("MediaProjectionService", "VirtualDisplay stopped")
        }
    }

    // Runnable periodically capturing screenshot from the existing ImageReader.
    private val captureRunnable = object : Runnable {
        override fun run() {
            captureScreenshot()
            handler.postDelayed(this, CAPTURE_DELAY)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Service")
            .setContentText("Capturing screen every 10 seconds...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        handler = Handler(Looper.getMainLooper())
        handler.post(appBlockerCheckRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra("data") && intent.hasExtra("resultCode")) {
            val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            val data = intent.getParcelableExtra<Intent>("data")
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!) ?: return START_NOT_STICKY
            mediaProjection?.registerCallback(mediaProjectionCallback, handler)
            MediaProjectionHolder.mediaProjection = mediaProjection

            // Create virtual display and image reader once.
            createVirtualDisplay()

            // Begin periodic capture.
            handler.post(captureRunnable)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(appBlockerCheckRunnable)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createVirtualDisplay() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        // Create ImageReader once with desired dimensions.
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            virtualDisplayCallback,
            handler
        )
    }

    // ...existing code...
    private fun captureScreenshot() {
        if (imageReader == null) {
            Log.e("MediaProjectionService", "ImageReader is not initialized")
            return
        }

        var image: Image? = null
        try {
            image = imageReader!!.acquireLatestImage()
            if (image != null) {
                val width = image.width
                val height = image.height
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val bitmapWidth = width + rowPadding / pixelStride

                val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                // Crop the bitmap to the exact screen size.
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)

                // Classify the screenshot.
                val classificationOutput = ImageClassifier.classifyImage(croppedBitmap)
                Log.d("MediaProjectionService", "Classification output: ${classificationOutput.contentToString()}")

                if (classificationOutput.isNotEmpty() && classificationOutput[0] > 0.5f) {
                    Log.d("MediaProjectionService", "⚠️ EXPLICIT CONTENT DETECTED ⚠️")

                    // Use more reliable fallback method
                    AppBlocker.blockCurrentAppWithFallback(this)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaProjectionService", "Error capturing screenshot", e)
        } finally {
            image?.close()
        }
    }
// ...existing code...

    private fun saveScreenshot(bitmap: Bitmap) {
        try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val screenshotsDir = File(picturesDir, "Screenshots")
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(screenshotsDir, "screenshot_$timeStamp.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("MediaProjectionService", "Screenshot saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("MediaProjectionService", "Error saving screenshot", e)
        }
    }

    private val appBlockerCheckRunnable = object : Runnable {
        override fun run() {
            if (AppBlocker.hasUsageStatsPermission(this@MediaProjectionService)) {
                AppBlocker.checkAndBlockIfNeeded(this@MediaProjectionService)
            }
            handler.postDelayed(this, 1000) // Check every second
        }
    }
}