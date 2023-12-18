package com.dmcroww.genderstatus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class StorageManager {
	companion object {
		/**
		 * Load person status from user storage
		 * @param partner If load partner, or user
		 */
		private fun getPerson(context: Context, partner: Boolean): Person {
			val storage = context.getSharedPreferences(if (partner) "partnerStorage" else "selfStorage", Context.MODE_PRIVATE)
			return Person(
				storage.getString("username", "") ?: "", storage.getString("avatar", "") ?: "", storage.getString("activity", "") ?: "", storage.getString("mood", "") ?: "", storage.getInt("age", 0), storage.getInt("sus", 0), storage.getInt("gender", 0), storage.getLong("timestamp", 0L)
			)
		}

		/**
		 * Load partner status from storage
		 */
		fun getPartner(context: Context): Person {
			return getPerson(context, true)
		}

		/**
		 * Load partner history from storage
		 */
		fun getPartnerHistory(context: Context): JSONArray {
			return JSONArray(context.getSharedPreferences("partnerHistoryStorage", Context.MODE_PRIVATE).getString("jsonArray","[]"))
		}

		/**
		 * Load user status from storage
		 */
		fun getUser(context: Context): Person {
			return getPerson(context, false)
		}

		/**
		 * Save updated user status to user storage
		 * @param person user's Person class to save
		 */
		private fun savePerson(context: Context, person: Person, partner: Boolean) {
			val key = if (partner) "partnerStorage" else "selfStorage"
			context.getSharedPreferences(key, Context.MODE_PRIVATE).edit().putString("username", person.username).putString("avatar", person.avatar).putString("activity", person.activity).putString("mood", person.mood).putInt("age", person.age).putInt("sus", person.sus).putInt("gender", person.gender).putLong("timestamp", person.timestamp).apply()
		}

		/**
		 * Save partner status to storage
		 */
		fun savePartner(context: Context,person: Person) {
			savePerson(context,person, true)
		}
		/**
		 * Save partner history to storage
		 */
		fun savePartnerHistory(context: Context,history: JSONArray) {
			context.getSharedPreferences("partnerHistoryStorage", Context.MODE_PRIVATE)
				.edit()
				.putString("jsonArray",history.toString())
				.apply()
		}

		/**
		 * Load user status from storage
		 */
		fun saveUser(context: Context,person: Person) {
			savePerson(context, person,false)
		}

		/**
		 * Get image from server, save in cache and return Bitmap of it
		 * @param folder folder (type) of image
		 * @param fileName name of the file on server
		 */
		suspend fun fetchImage(context: Context, folder: String, fileName: String, force: Boolean = false): Bitmap? {
			return try {
				if (fileName.isBlank()) return null

				val filePath = "$folder/$fileName"
				val cacheFile = File(context.cacheDir, filePath)

				if (cacheFile.exists() && !force) {
					BitmapFactory.decodeFile(cacheFile.absolutePath)
				} else {
					withContext(Dispatchers.IO) {
						val inputStream = URL("https://api.dmcroww.live/genderStatus/$filePath").openStream()
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
}

/**
 * Person class to manage easier saving and loading.
 */
data class Person(
	val username: String, var avatar: String, var activity: String, var mood: String, var age: Int, var sus: Int, var gender: Int, var timestamp: Long
) {}

data class AppOptions(
	var username: String, var partner: String, var background: String, var textColorInt: Int, var updateInterval: Int, var fontSize: Int, var lastCacheTs: Long
) {
	companion object {
		private const val APP_DATA_KEY = "appStorage"
		fun getData(context: Context): AppOptions {
			val storage = context.getSharedPreferences(APP_DATA_KEY, Context.MODE_PRIVATE)
			return AppOptions(
				storage.getString("username", "")!!,
				storage.getString("partner", "")!!,
				storage.getString("background", "")!!,
				storage.getInt("color", 0),
				storage.getInt("updateInterval", 5),
				storage.getInt("fontSize", 100),
				storage.getLong("lastCacheTs", 0L)
			)
		}

		fun saveData(context: Context, options: AppOptions) {
			context.getSharedPreferences(APP_DATA_KEY, Context.MODE_PRIVATE).edit()
				.putString("username", options.username)
				.putString("partner", options.partner)
				.putString("background", options.background)
				.putInt("color", options.textColorInt)
				.putInt("updateInterval", options.updateInterval)
				.putInt("fontSize", options.fontSize)
				.putLong("lastCacheTs", options.lastCacheTs)
				.apply()
		}
	}
}
