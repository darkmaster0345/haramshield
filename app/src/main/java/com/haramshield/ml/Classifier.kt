package com.haramshield.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Classifier @Inject constructor(
    private val context: Context
) {

    private var interpreter: Interpreter? = null
    private var imageProcessor: ImageProcessor? = null
    private val modelFilename = "nsfw.tflite"

    init {
        initialize()
    }

    private fun initialize() {
        try {
            val modelBuffer = loadModelFile(modelFilename)
            interpreter = Interpreter(modelBuffer)
            
            // Standard NSFW Model (GantMan) expects 224x224
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f)) // Normalize to [0, 1]
                .build()
                
            Timber.d("Haram Detector Classifier initialized with $modelFilename")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Haram Detector Classifier")
        }
    }

    fun checkScreen(bitmap: Bitmap): HaramType {
        val currentInterpreter = interpreter ?: return HaramType.SAFE

        try {
            val tensorImage = TensorImage(DataType.FLOAT32) 
            tensorImage.load(bitmap)
            val processedImage = imageProcessor?.process(tensorImage) ?: return HaramType.SAFE

            // Output probability buffer: 1 batch, 5 classes 
            // [Drawing, Hentai, Neutral, Porn, Sexy]
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 5), DataType.FLOAT32)
            
            currentInterpreter.run(processedImage.buffer, outputBuffer.buffer.rewind())
            
            val probabilities = outputBuffer.floatArray
            
            // Mapping GantMan Model to HaramType
            val drawing = probabilities[0]
            val hentai = probabilities[1]
            val neutral = probabilities[2]
            val porn = probabilities[3]
            val sexy = probabilities[4]
            
            val nsfwScore = hentai + porn + sexy
            
            Timber.d("NSFW Scan: P=$porn H=$hentai S=$sexy | Safe: N=$neutral D=$drawing")

            // Threshold check (0.65f strict)
            if (nsfwScore > 0.65f) {
                return HaramType.NSFW
            }
            
            // TODO: Alcohol detection requires a separate model or secondary head. 
            // For now, only NSFW is active as per user priority.
            
        } catch (e: Exception) {
            Timber.e(e, "Error during inference")
        }
        
        return HaramType.SAFE
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        return FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }
    
    fun close() {
        interpreter?.close()
    }
}
