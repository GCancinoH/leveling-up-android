package com.gcancino.levelingup.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AssetsUtil {

    private const val TAG = "VoskModelUnpacker"

    /**
     * Unpacks a folder from the assets directory to the app's external files directory.
     * This is necessary because the Vosk model needs to be loaded from a file path,
     * not directly from the assets.
     *
     * @param context The application context.
     * @param assetFolderName The name of the folder in the assets directory to unpack.
     * @return The absolute path to the unpacked folder on the device, or null on failure.
     */
    fun unpackAssetsFolder(context: Context, assetFolderName: String): String? {
        val outputDir = File(context.filesDir, assetFolderName)

        // A simple check to see if a key model file exists. This avoids re-unzipping on every app start.
        // For a production app, a more robust version check might be needed.
        val modelCheckFile = File(outputDir, "am/final.mdl")
        if (modelCheckFile.exists()) {
            Log.d(TAG, "Model folder '$assetFolderName' already exists. Skipping unpacking.")
            return outputDir.absolutePath
        }

        // If the directory doesn't exist or was partially created, clean it up and recreate.
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        if (!outputDir.mkdirs()) {
            Log.e(TAG, "Failed to create directory: ${outputDir.absolutePath}")
            return null
        }

        Log.d(TAG, "Starting to unpack model '$assetFolderName' to '${outputDir.absolutePath}'.")

        try {
            val assetManager = context.assets
            // Recursively copy files from the assets folder to the output directory
            copyAssetFolder(assetManager, assetFolderName, outputDir.absolutePath)
            Log.d(TAG, "Successfully unpacked model '$assetFolderName'.")
            return outputDir.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to unpack model '$assetFolderName'.", e)
            Timber.tag(TAG).d("Successfully unpacked model '$assetFolderName'.")
            return outputDir.absolutePath
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Failed to unpack model '$assetFolderName'.")
            // Clean up on failure
            outputDir.deleteRecursively()
            return null
        }
    }

    @Throws(IOException::class)
    private fun copyAssetFolder(assetManager: android.content.res.AssetManager, fromAssetPath: String, toPath: String) {
        val files = assetManager.list(fromAssetPath)
        if (files.isNullOrEmpty()) {
            // It's a file, not a directory
            copyAssetFile(assetManager, fromAssetPath, toPath)
        } else {
            // It's a directory
            val dir = File(toPath)
            if (!dir.exists() && !dir.mkdirs()) {
                throw IOException("Failed to create directory: $toPath")
            }
            for (file in files) {
                copyAssetFolder(assetManager, "$fromAssetPath/$file", "$toPath/$file")
            }
        }
    }

    @Throws(IOException::class)
    private fun copyAssetFile(assetManager: android.content.res.AssetManager, fromAssetPath: String, toPath: String) {
        assetManager.open(fromAssetPath).use { inputStream ->
            FileOutputStream(toPath).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}
