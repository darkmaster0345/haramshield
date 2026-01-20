package com.haramshield.ml

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for preprocessing images for ML model input
 */
@Singleton
class ImagePreprocessor @Inject constructor() {
    
    /**
     * Preprocess bitmap for NSFW model (typically 224x224 or 256x256)
     */
    fun preprocessForNsfw(bitmap: Bitmap, targetSize: Int = 224): ByteBuffer {
        return preprocessImage(bitmap, targetSize, normalize = true)
    }
    
    /**
     * Preprocess bitmap for object detection model (typically 320x320 or 640x640)
     */
    fun preprocessForObjectDetection(bitmap: Bitmap, targetSize: Int = 320): ByteBuffer {
        return preprocessImage(bitmap, targetSize, normalize = true)
    }
    
    /**
     * Generic image preprocessing
     */
    private fun preprocessImage(
        bitmap: Bitmap,
        targetSize: Int,
        normalize: Boolean = true
    ): ByteBuffer {
        // Resize the bitmap
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        
        // Allocate ByteBuffer for float input (4 bytes per float, 3 channels)
        val inputBuffer = ByteBuffer.allocateDirect(targetSize * targetSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Convert pixels to normalized float values
        val pixels = IntArray(targetSize * targetSize)
        resizedBitmap.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)
        
        for (pixel in pixels) {
            // Extract RGB values
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)
            
            if (normalize) {
                // Normalize to [0, 1] or [-1, 1] depending on model requirements
                inputBuffer.putFloat(r / 255.0f)
                inputBuffer.putFloat(g / 255.0f)
                inputBuffer.putFloat(b / 255.0f)
            } else {
                inputBuffer.putFloat(r.toFloat())
                inputBuffer.putFloat(g.toFloat())
                inputBuffer.putFloat(b.toFloat())
            }
        }
        
        // Clean up if we created a new bitmap
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        
        inputBuffer.rewind()
        return inputBuffer
    }
    
    /**
     * Resize bitmap maintaining aspect ratio, padding if necessary
     */
    fun resizeWithPadding(bitmap: Bitmap, targetSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = targetSize.toFloat() / maxOf(width, height)
        
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        // Create padded bitmap
        val paddedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(paddedBitmap)
        canvas.drawColor(android.graphics.Color.BLACK) // Fill with black
        
        val left = (targetSize - scaledWidth) / 2
        val top = (targetSize - scaledHeight) / 2
        canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), null)
        
        scaledBitmap.recycle()
        return paddedBitmap
    }
    
    /**
     * Center crop a bitmap to target size
     */
    fun centerCrop(bitmap: Bitmap, targetSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val minDimension = minOf(width, height)
        
        // Scale so smallest dimension matches target
        val scale = targetSize.toFloat() / minDimension
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        // Center crop
        val x = (scaledWidth - targetSize) / 2
        val y = (scaledHeight - targetSize) / 2
        
        val croppedBitmap = Bitmap.createBitmap(scaledBitmap, x, y, targetSize, targetSize)
        
        if (scaledBitmap != croppedBitmap) {
            scaledBitmap.recycle()
        }
        
        return croppedBitmap
    }
}
