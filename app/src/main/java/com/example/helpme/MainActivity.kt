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
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
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

        val permissions = mutableListOf<String>()

        // Check for usage stats permission
        if (!AppBlocker.hasUsageStatsPermission(this)) {
            // Show alert explaining permission need
            android.app.AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Help Me needs access to usage statistics to block apps when explicit content is detected.")
                .setPositiveButton("Grant") { _, _ ->
                    AppBlocker.requestUsageStatsPermission(this)
                }
                .setCancelable(false)
                .show()
        }

        // Check for overlay permission
        if (!Settings.canDrawOverlays(this)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("Help Me needs to display over other apps to block inappropriate content.")
                .setPositiveButton("Grant") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivity(intent)
                }
                .setCancelable(false)
                .show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Request overlay permission if not granted
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }

        // Request usage stats permission
        if (!AppBlocker.hasUsageStatsPermission(this)) {
            AppBlocker.requestUsageStatsPermission(this)
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)


        // Request all needed storage permissions in a single call
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)

        ImageClassifier.loadModel(this, "HelpNet_V2_Android.ptl")

        // Register the activity result launcher for MediaProjection permission
        startMediaProjection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Start the MediaProjectionService with the granted permission.
                val serviceIntent = Intent(this, MediaProjectionService::class.java).apply {
                    putExtra("resultCode", result.resultCode)
                    putExtra("data", result.data)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
                
                // Trigger going to the Home screen after recording permission is granted.
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        }

        // Create and launch the intent for screen capture
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startMediaProjection.launch(captureIntent)
    }
}

