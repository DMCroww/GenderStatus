package com.dmcroww.genderstatus.providers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class StorageManager(private val context: Context) {
	/**
	 * Get avatar image from server, save in cache and return Bitmap of it
	 * @param fileName name of the file on server
	 * @param force skip cache check and refresh from server
	 */
	suspend fun fetchAvatar(fileName: String, force: Boolean = false): Bitmap? {
		return try {
			if (fileName.isBlank()) return null

			val filePath = "avatars/$fileName"
			val cacheFile = File(context.cacheDir, filePath)
			if (cacheFile.exists() && !force) {
				BitmapFactory.decodeFile(cacheFile.absolutePath)
			} else {
				withContext(Dispatchers.IO) {
					val inputStream = URL("https://api.dmcroww.tech/genderStatus/v2/$filePath").openStream()
					val image = BitmapFactory.decodeStream(inputStream)
					inputStream.close()
					cacheFile.parentFile?.mkdirs()
					FileOutputStream(cacheFile).use {out ->
						image.compress(Bitmap.CompressFormat.PNG, 100, out)
					}

					Log.d("SM", "Image saved to cache: ${cacheFile.absolutePath}")
					image
				}
			}
		} catch (e: Exception) {
			Log.e("SM", "Error saving image: ${e.message}")
			null
		}
	}

	/**
	 * Get background image from server, save in cache and return Bitmap of it
	 * @param fileName name of the file on server
	 * @param force skip cache check and refresh from server
	 */
	suspend fun fetchBackground(fileName: String, force: Boolean = false): Bitmap? {
		return try {
			if (fileName.isBlank()) return null

			val filePath = "backgrounds/$fileName"

			val cacheFile = File(context.cacheDir, filePath)

			if (cacheFile.exists() && !force) {
				BitmapFactory.decodeFile(cacheFile.absolutePath)
			} else {
				withContext(Dispatchers.IO) {
					val inputStream = URL("https://api.dmcroww.tech/genderStatus/v2/$filePath").openStream()
					val image = BitmapFactory.decodeStream(inputStream)
					inputStream.close()
					cacheFile.parentFile?.mkdirs()
					FileOutputStream(cacheFile).use {out ->
						image.compress(Bitmap.CompressFormat.PNG, 100, out)
					}

					Log.d("SM", "Image saved to cache: ${cacheFile.absolutePath}")
					image
				}
			}
		} catch (e: Exception) {
			Log.e("SM", "Error saving image: ${e.message}")
			null
		}
	}
}
