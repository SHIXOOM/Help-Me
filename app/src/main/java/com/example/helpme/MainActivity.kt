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
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.helpme.ui.theme.HelpMeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.exp


class MainActivity : ComponentActivity() {

    private lateinit var module: Module
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

        // Request all needed storage permissions in a single call
        val permissions = mutableListOf<String>()
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

        // Load the PyTorch model in a background thread
//        CoroutineScope(Dispatchers.IO).launch {
//            module = Module.load(assetFilePath("HelpNet_V1_CPU.pt"))
//        }

        ImageClassifier.loadModel(this, "HelpNet_V1_CPU.pt")

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



    // Helper to load PyTorch's model
    private fun assetFilePath(assetName: String): String {
        val file = File(filesDir, assetName)
        assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
        return file.absolutePath
    }

    // Preprocess the image
    fun preprocessImage(bitmap: Bitmap): Tensor {
        // Resize the image to 256x256
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)

        // Center crop the image to 224x224
        val cropX = (resizedBitmap.width - 224) / 2
        val cropY = (resizedBitmap.height - 224) / 2
        val croppedBitmap = Bitmap.createBitmap(resizedBitmap, cropX, cropY, 224, 224)

        // Convert the cropped bitmap into a tensor and normalize it
        return TensorImageUtils.bitmapToFloat32Tensor(
            croppedBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
    }

    // Moved outside of preprocessImage so it is accessible
    fun classifyImage(bitmap: Bitmap): FloatArray {
        if (!::module.isInitialized) {
            Log.d("MainActivity", "Model not loaded yet")
            return floatArrayOf()
        }
        // Preprocess the image
        val inputTensor = preprocessImage(bitmap)

        // Perform inference
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()

        // Apply sigmoid function manually
        val outputData = outputTensor.dataAsFloatArray
        val sigmoidOutputData = outputData.map { 1 / (1 + exp(-it)) }.toFloatArray()

        // Log that the model has run along with the model's output
        Log.d("MainActivity", "Model has run. Output: ${sigmoidOutputData.contentToString()}")

        return sigmoidOutputData
    }


}

