/**
 * MainActivity is the application's entry point. It performs the following key functions:
 * - Sets up the UI using Jetpack Compose.
 * - Requests required storage permissions based on the OS version.
 * - Loads a PyTorch neural network model from the assets in a background thread.
 * - Initiates screen capturing through media projection.
 * - Schedules automated screenshot capture every 15 seconds, processes the screenshot 
 *   through the neural network, and logs the model's output.
 */
 
/**
 * Called when the activity is first created.
 *
 * Responsibilities:
 * - Enable edge-to-edge drawing and set up the Compose UI.
 * - Request necessary storage permissions (both for writing and reading images).
 * - Load the neural network model from the assets in a background I/O thread.
 * - Start media projection by launching the screen capture intent.
 * - Initialize a Handler and a Runnable to capture and process screenshots periodically.
 *
 * @param savedInstanceState Bundle for restoring activity state, if available.
 */
 
/**
 * Called when the activity is destroyed.
 *
 * Responsibilities:
 * - Remove any pending screenshot capture callbacks.
 * - Stop the media projection to release resources.
 */
 
/**
 * Configures the screen capturing environment.
 *
 * Responsibilities:
 * - Retrieve screen dimensions and density.
 * - Configure an ImageReader to capture the current screen.
 * - Set up a VirtualDisplay to push the screen content into the ImageReader's surface.
 */
 
/**
 * Captures a screenshot from the configured ImageReader.
 *
 * Functionality:
 * - Acquires the latest image from the ImageReader.
 * - Constructs a Bitmap from the image buffer, accounting for any row padding.
 * - Processes the bitmap through the neural network model by calling classifyImage.
 * - Logs and prints the resulting model output.
 */
 
/**
 * Saves the provided bitmap as a screenshot to external storage.
 *
 * Process:
 * - Generates a timestamp-based filename.
 * - Creates the "Screenshots" directory under the public Pictures directory if it does not exist.
 * - Writes the bitmap data to the file and logs the saved file's absolute path.
 * - Handles and logs any IOException that may occur during file I/O.
 *
 * @param bitmap The bitmap image to be saved.
 */
 
/**
 * Copies an asset file to the internal storage and returns its absolute file path.
 *
 * Process:
 * - Opens the asset file as an InputStream.
 * - Writes the content to a file in the app's files directory using an OutputStream.
 * - Flushes the output and returns the absolute path of the saved file.
 *
 * @param assetName The name of the asset file.
 * @return The absolute file path of the copied asset.
 */
 
/**
 * Preprocesses a bitmap image for input into the neural network model.
 *
 * Process:
 * - Resizes the bitmap to 256x256 pixels.
 * - Performs a center-crop to obtain a 224x224 pixel image.
 * - Converts the cropped bitmap into a tensor and normalizes it using standard mean and std values.
 *
 * @param bitmap The original bitmap image to preprocess.
 * @return A Tensor suitable for model inference.
 */
 
/**
 * Performs inference on the provided bitmap image using the loaded PyTorch model.
 *
 * Process:
 * - Calls preprocessImage to convert the bitmap into a normalized tensor.
 * - Executes the model forward pass to obtain the output tensor.
 * - Applies a sigmoid function to the output tensor values manually.
 * - Returns the resulting float array representing the model prediction.
 *
 * @param bitmap The image to classify.
 * @return A float array containing the sigmoid-activated model output.
 */
 
/**
 * MediaProjectionService is a foreground service responsible for maintaining the media projection session.
 *
 * Functionality:
 * - Creates a notification channel and starts itself in the foreground.
 * - Ensures that media projection continues running.
 */
 
/**
 * Called when the service is created.
 *
 * Responsibilities:
 * - Create a notification channel for the media projection service.
 * - Start the service as a foreground service using a notification to keep the projection alive.
 */
 
/**
 * Called when the service is started.
 *
 * Responsibilities:
 * - Returns START_STICKY to indicate that the service should continue running until explicitly stopped.
 *
 * @param intent The intent supplied to start the service.
 * @param flags Additional data about the start request.
 * @param startId A unique integer representing this specific request to start.
 * @return START_STICKY constant to ensure the service remains active.
 */
 
/**
 * Called when a component wants to bind to the service.
 *
 * As binding is not supported in this service, it returns null.
 *
 * @param intent The intent used to bind to the service.
 * @return Always returns null as binding is not provided.
 */
 
/**
 * A simple composable function that displays a greeting text.
 *
 * @param name The string to display as part of the greeting.
 * @param modifier A [Modifier] for styling and layout adjustments.
 */
 
/**
 * A preview composable function for the Greeting function.
 *
 * This enables previewing the Greeting composable in the IDE's design view.
 */
package com.example.helpme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle

import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.Settings


class MainActivity : ComponentActivity() {

    private lateinit var imageReader: ImageReader

    // Permission tracking
    private var permissionStep = 0 // 0=none, 1=usage stats, 2=overlay, 3=projection

    // Assuming these values are defined somewhere in your class
    private val displayMetrics: DisplayMetrics by lazy {
        DisplayMetrics().apply { windowManager.defaultDisplay.getMetrics(this) }
    }

    private val screenWidth: Int
        get() = displayMetrics.widthPixels

    private val screenHeight: Int
        get() = displayMetrics.heightPixels

    private val screenDensity: Int
        get() = displayMetrics.densityDpi

    private lateinit var startMediaProjection: ActivityResultLauncher<Intent>
    


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d("MainActivity", "Starting sequential permission requests")
        
        // Initialize the media projection launcher
        startMediaProjection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                Log.d("MainActivity", "Screen capture permission granted - Final step")
                
                // Start the MediaProjectionService with the granted permission
                val serviceIntent = Intent(this, MediaProjectionService::class.java).apply {
                    putExtra("resultCode", result.resultCode)
                    putExtra("data", result.data)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
                
                // Only NOW go to home screen after all permissions have been granted
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            } else {
                Log.d("MainActivity", "Screen capture permission denied")
            }
        }
        
        // Load ML model in background
        ImageClassifier.loadModel(this, "HelpNet_V2_Android.ptl")
        
        // Begin permission sequence with step 1
        permissionStep = 1
        requestNextPermission()

    }
    
    /**
     * Handles requesting the next permission in sequence based on permissionStep
     */
    private fun requestNextPermission() {
        when (permissionStep) {
            1 -> requestUsageStatsPermission()
            2 -> requestOverlayPermission()
            3 -> requestMediaProjectionPermission()
        }
    }
    

    
    /**
     * Step 1: Request usage stats permission
     */
    private fun requestUsageStatsPermission() {
        Log.d("MainActivity", "Step 1: Requesting usage stats permission")
        
        if (!AppBlocker.hasUsageStatsPermission(this)) {
            // Show alert explaining permission need
            android.app.AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Help Me needs access to usage statistics to block apps when explicit content is detected.")
                .setPositiveButton("Grant") { _, _ ->
                    AppBlocker.requestUsageStatsPermission(this)
                    // We'll rely on onResume to continue the flow
                }
                .setCancelable(false)
                .show()
        } else {
            // Already granted, move to next permission
            permissionStep = 2
            requestNextPermission()
        }
    }
    
    /**
     * Step 2: Request overlay permission
     */
    private fun requestOverlayPermission() {
        Log.d("MainActivity", "Step 2: Requesting overlay permission")
        
        if (!Settings.canDrawOverlays(this)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("Help Me needs to display over other apps to block inappropriate content.")
                .setPositiveButton("Grant") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                    // We'll rely on onResume to continue the flow
                }
                .setCancelable(false)
                .show()
        } else {
            // Already granted, move to final permission
            permissionStep = 3
            requestNextPermission()
        }
    }
    
    /**
     * Step 3 (Final): Request media projection permission
     * This is the final step that will trigger the app to go to home screen
     * after this permission is granted
     */
    private fun requestMediaProjectionPermission() {
        Log.d("MainActivity", "Step 3 (Final): Requesting screen capture permission")
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startMediaProjection.launch(captureIntent)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check permissions after returning from system settings
        when (permissionStep) {
            1 -> {
                // Check if usage stats permission is now granted
                if (AppBlocker.hasUsageStatsPermission(this)) {
                    permissionStep = 2
                    requestNextPermission()
                }
            }
            2 -> {
                // Check if overlay permission is now granted
                if (Settings.canDrawOverlays(this)) {
                    permissionStep = 3
                    requestNextPermission()
                }
            }
        }
    }
    

}

