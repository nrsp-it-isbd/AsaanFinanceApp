package com.assan.finance.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    private const val JPEG_QUALITY = 85
    private const val MAX_WIDTH = 1920

    /**
     * Compress a bitmap from a URI to JPEG at 85% quality.
     * Resizes if width > 1920px maintaining aspect ratio.
     * Corrects EXIF rotation.
     * Returns a File in the cache directory.
     */
    suspend fun compress(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        // Decode bitmap from URI
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open URI: $uri")

        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Read EXIF rotation from URI
        val rotationDegrees = getExifRotation(context, uri)

        // Apply EXIF rotation if needed
        val rotatedBitmap = if (rotationDegrees != 0f) {
            rotateBitmap(originalBitmap, rotationDegrees)
        } else {
            originalBitmap
        }

        // Resize if too large
        val resizedBitmap = if (rotatedBitmap.width > MAX_WIDTH) {
            resizeBitmap(rotatedBitmap, MAX_WIDTH)
        } else {
            rotatedBitmap
        }

        // Prepare output file
        val cacheDir = File(context.cacheDir, "compressed").apply { mkdirs() }
        val outputFile = File(cacheDir, "plant_${System.currentTimeMillis()}.jpg")

        // Compress to JPEG
        FileOutputStream(outputFile).use { out ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        // Recycle bitmaps
        if (resizedBitmap != rotatedBitmap) resizedBitmap.recycle()
        if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
        originalBitmap.recycle()

        outputFile
    }

    private fun getExifRotation(context: Context, uri: Uri): Float {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0f
            val exif = ExifInterface(inputStream)
            inputStream.close()
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (maxWidth * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)
    }
}
