package com.jnetai.puzzle.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * ImageLibraryUtils - helpers for listing and decoding built-in asset images
 * and user-uploaded URIs. All failures are logged with a persistent error code.
 */
object ImageLibraryUtils {

    const val ASSET_DIR = "images"

    /**
     * List every built-in image filename in the assets/images folder.
     * Returns an alphabetically sorted (natural order) list of names.
     */
    fun listBuiltInImages(context: Context): List<String> {
        return try {
            val names = context.assets.list(ASSET_DIR) ?: emptyArray()
            val sorted = names.filter { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") }
                .sortedWith(compareBy { it.toNaturalKey() })
            ErrorLogger.logf(ErrorLogger.Codes.IMG_ASSET_LIST,
                "Found %d built-in images in assets", sorted.size)
            sorted
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.IMG_ASSET_LIST, "Failed to list asset images", e)
            emptyList()
        }
    }

    /** Natural-number sort key so "2.jpg" sorts before "10.jpg". */
    private fun String.toNaturalKey(): String {
        return Regex("\\d+").replace(this) { it.value.padStart(5, '0') }
    }

    /** Glide-compatible URI for a built-in asset image. */
    fun assetUri(name: String): android.net.Uri {
        return android.net.Uri.parse("file:///android_asset/$ASSET_DIR/$name")
    }

    /**
     * Decode a built-in asset image as a sampled bitmap no larger than maxSize px
     * on its longest edge, so memory stays bounded for the puzzle board.
     */
    fun loadAssetImage(context: Context, name: String, maxSize: Int = 1400): Bitmap? {
        return try {
            val input = context.assets.open("$ASSET_DIR/$name")
            val source = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, source)
            input.close()

            val sample = computeSampleSize(source.outWidth, source.outHeight, maxSize)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val input2 = context.assets.open("$ASSET_DIR/$name")
            val bitmap = BitmapFactory.decodeStream(input2, null, opts)
            input2.close()
            ErrorLogger.logf(ErrorLogger.Codes.IMG_ASSET_LOAD,
                "Loaded asset '%s' (%dx%d sample=%d)",
                name, source.outWidth, source.outHeight, sample)
            bitmap
        } catch (e: Exception) {
            ErrorLogger.logf(ErrorLogger.Codes.IMG_ASSET_LOAD,
                "Failed to load asset image '%s'", e, name)
            null
        }
    }

    /**
     * Decode a user-selected image URI into a sampled bitmap. Uses the
     * content resolver via BitmapFactory.
     */
    fun loadUriImage(context: Context, uri: Uri, maxSize: Int = 1400): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val input = resolver.openInputStream(uri) ?: return null
            val source = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, source)
            input.close()

            if (source.outWidth <= 0 || source.outHeight <= 0) {
                ErrorLogger.logf(ErrorLogger.Codes.IMG_URI_DECODE,
                    "URI '%s' has invalid dimensions (%dx%d)", uri, source.outWidth, source.outHeight)
                return null
            }

            val sample = computeSampleSize(source.outWidth, source.outHeight, maxSize)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val input2 = resolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input2, null, opts)
            input2.close()
            ErrorLogger.logf(ErrorLogger.Codes.IMG_URI_DECODE,
                "Decoded URI '%s' (%dx%d sample=%d)",
                uri, source.outWidth, source.outHeight, sample)
            bitmap
        } catch (e: Exception) {
            ErrorLogger.logf(ErrorLogger.Codes.IMG_URI_DECODE,
                "Failed to decode URI image '%s'", e, uri)
            null
        }
    }

    /** Work out a power-of-two sample size to keep the long edge near maxSize. */
    private fun computeSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / (sample * 2) >= maxSize) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    /**
     * Persist a decoded bitmap to the app cache directory so the game can
     * re-load it (e.g. across rotations). Returns the file path.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val file = File(context.cacheDir, fileName)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            ErrorLogger.logf(ErrorLogger.Codes.IMG_SAVE_CACHE,
                "Saved bitmap to cache: %s (%d bytes)", file.absolutePath, file.length())
            file.absolutePath
        } catch (e: Exception) {
            ErrorLogger.logf(ErrorLogger.Codes.IMG_SAVE_CACHE,
                "Failed to save bitmap to cache", e)
            null
        }
    }

    /**
     * Decode a previously saved cache file, or null if it no longer exists.
     */
    fun loadCachedBitmap(context: Context, fileName: String, maxSize: Int = 1400): Bitmap? {
        return try {
            val file = File(context.cacheDir, fileName)
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, maxSize)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (e: Exception) {
            ErrorLogger.logf(ErrorLogger.Codes.IMG_SAMPLE, "Failed to load cached bitmap", e)
            null
        }
    }
}