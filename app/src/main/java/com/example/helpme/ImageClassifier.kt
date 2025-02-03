package com.example.helpme

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.exp

object ImageClassifier {
    private const val TAG = "ImageClassifier"
    private var module: Module? = null

    /**
     * Loads a PyTorch model from the assets folder.
     * Must be called before classifyImage is used.
     *
     * @param context The context to access assets and files.
     * @param assetName The name of the asset file (e.g., "HelpNet_V1_CPU.pt").
     */
    fun loadModel(context: Context, assetName: String) {
        try {
            val modelFilePath = assetFilePath(context, assetName)
            module = Module.load(modelFilePath)
            Log.d(TAG, "Model loaded from: $modelFilePath")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
        }
    }

    /**
     * Copies an asset file to internal storage and returns its absolute file path.
     *
     * @param context The context to access assets and files.
     * @param assetName The asset file name.
     * @return The absolute file path of the copied file.
     */
    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath

        try {
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Error processing asset file $assetName", e)
        }
        return file.absolutePath
    }

    /**
     * Preprocesses the given Bitmap into a Tensor.
     *
     * Process:
     * - Resize the image to 256x256.
     * - Center-crop the image to 224x224.
     * - Normalize the image using standard mean and std.
     *
     * @param bitmap The image bitmap.
     * @return A Tensor ready for model input.
     */
    fun preprocessImage(bitmap: Bitmap): Tensor {
        // Resize the image to 256x256
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        // Center crop to 224x224
        val cropX = (resizedBitmap.width - 224) / 2
        val cropY = (resizedBitmap.height - 224) / 2
        val croppedBitmap = Bitmap.createBitmap(resizedBitmap, cropX, cropY, 224, 224)

        // Convert the cropped bitmap into a tensor.
        return TensorImageUtils.bitmapToFloat32Tensor(
            croppedBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
    }

    /**
     * Classifies the provided Bitmap using the loaded model.
     *
     * Process:
     * - Preprocess the image.
     * - Run inference.
     * - Apply sigmoid activation to the output.
     *
     * @param bitmap The image bitmap.
     * @return A float array containing the sigmoid-activated output.
     */
    fun classifyImage(bitmap: Bitmap): FloatArray {
        if (module == null) {
            Log.e(TAG, "Model not loaded. Call loadModel first.")
            return floatArrayOf()
        }
        // Preprocess the image to create input tensor.
        val inputTensor = preprocessImage(bitmap)

        // Run the model inference.
        val outputTensor = module!!.forward(IValue.from(inputTensor)).toTensor()
        val outputData = outputTensor.dataAsFloatArray

        // Apply sigmoid activation to each output value.
        val sigmoidOutput = outputData.map { 1 / (1 + exp(-it)) }.toFloatArray()

        Log.d(TAG, "Classification output: ${sigmoidOutput.contentToString()}")
        return sigmoidOutput
    }
}