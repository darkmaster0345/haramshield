package com.haramshield.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import timber.log.Timber
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages loading and lifecycle of TensorFlow Lite models
 */
@Singleton
class ModelManager @Inject constructor(
    private val context: Context
) {
    
    private var nsfwInterpreter: Interpreter? = null
    private var objectInterpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    
    private val isGpuAvailable: Boolean by lazy {
        CompatibilityList().isDelegateSupportedOnThisDevice
    }
    
    /**
     * Initialize all ML models
     */
    fun initialize() {
        try {
            initializeGpuDelegate()
            loadNsfwModel()
            loadObjectDetectionModel()
            Timber.d("ML models initialized successfully (GPU: $isGpuAvailable)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ML models")
        }
    }
    
    private fun initializeGpuDelegate() {
        if (isGpuAvailable) {
            try {
                gpuDelegate = GpuDelegate()
                Timber.d("GPU delegate initialized")
            } catch (e: Exception) {
                Timber.w(e, "Failed to initialize GPU delegate, falling back to CPU")
            }
        }
    }
    
    private fun loadNsfwModel() {
        try {
            val modelBuffer = loadModelFile(NSFW_MODEL_FILE)
            val options = createInterpreterOptions()
            nsfwInterpreter = Interpreter(modelBuffer, options)
            Timber.d("NSFW model loaded")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load NSFW model")
            // Create placeholder interpreter that returns safe results
        }
    }
    
    private fun loadObjectDetectionModel() {
        try {
            val modelBuffer = loadModelFile(OBJECT_DETECTION_MODEL_FILE)
            val options = createInterpreterOptions()
            objectInterpreter = Interpreter(modelBuffer, options)
            Timber.d("Object detection model loaded")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load object detection model")
        }
    }
    
    private fun createInterpreterOptions(): Interpreter.Options {
        return Interpreter.Options().apply {
            setNumThreads(NUM_THREADS)
            gpuDelegate?.let { addDelegate(it) }
        }
    }
    
    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("models/$fileName")
        return FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }
    
    fun getNsfwInterpreter(): Interpreter? = nsfwInterpreter
    
    fun getObjectInterpreter(): Interpreter? = objectInterpreter
    
    /**
     * Release all resources
     */
    fun release() {
        nsfwInterpreter?.close()
        objectInterpreter?.close()
        gpuDelegate?.close()
        nsfwInterpreter = null
        objectInterpreter = null
        gpuDelegate = null
        Timber.d("ML resources released")
    }
    
    companion object {
        private const val NSFW_MODEL_FILE = "nsfw_model.tflite"
        private const val OBJECT_DETECTION_MODEL_FILE = "object_detection.tflite"
        private const val NUM_THREADS = 4
    }
}
