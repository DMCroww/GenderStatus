package com.dmcroww.genderstatus

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class StorageManager(private val context: Context) {
	fun savePerson(person: Person) {
		context.getSharedPreferences("people." + person.username, MODE_PRIVATE)
			.edit()
			.putString("nickname", person.nickname)
			.putString("activity", person.status.json().toString())
			.putLong("timestamp", person.timestamp)
			.apply()
	}

	fun loadPerson(username: String): Person {
		val storage = context.getSharedPreferences("people.$username", MODE_PRIVATE)
		return Person(
			username,
			storage.getString("nickname", "").toString(),
			Status(JSONObject(storage.getString("status", "{}")!!)),
			storage.getLong("timestamp", 0L),
		)
	}

	/**
	 * Get image from server, save in cache and return Bitmap of it
	 * @param folder folder (type) of image
	 * @param fileName name of the file on server
	 */
	suspend fun fetchImage(folder: String, fileName: String, force: Boolean = false): Bitmap? {
		return try {
			if (fileName.isBlank()) return null

			val filePath = "$folder/$fileName"
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

/**
 * Person class to manage easier saving and loading.
 */
data class Person(
	var username: String,
	var nickname: String = "",
	var status: Status = Status(JSONObject()),
	var timestamp: Long = 0L,
)

/**
 * Status class to manage easier saving and loading.
 */
data class Status(val jsonObject: JSONObject) {
	var avatar: String = jsonObject.optString("avatar", "")
	var activity: String = jsonObject.optString("activity", "")
	var mood: String = jsonObject.optString("mood", "")
	var age: Int = jsonObject.optInt("age", 0)
	var sus: Int = jsonObject.optInt("sus", 0)
	var gender: Int = jsonObject.optInt("gender", 0)
	var timestamp: Long = jsonObject.optLong("timestamp", 0)

	fun json(): JSONObject {
		return JSONObject()
			.put("avatar", this.avatar)
			.put("activity", this.activity)
			.put("mood", this.mood)
			.put("age", this.age)
			.put("sus", this.sus)
			.put("gender", this.gender)
			.put("timestamp", this.timestamp)
	}
}

data class AppOptions(private val context: Context) {
	private val storage = context.getSharedPreferences("appData", MODE_PRIVATE)
	var background: String = storage.getString("background", "")!!
	var textColorInt: Int = storage.getInt("color", 0)
	var updateInterval: Int = storage.getInt("updateInterval", 10)
	var fontSize: Int = storage.getInt("fontSize", 100)
	var lastCacheTs: Long = storage.getLong("lastCacheTs", 0L)

	fun save() {
		storage.edit()
			.putString("background", this.background)
			.putInt("color", this.textColorInt)
			.putInt("updateInterval", this.updateInterval)
			.putInt("fontSize", this.fontSize)
			.putLong("lastCacheTs", this.lastCacheTs)
			.apply()
	}

	fun load() {
		this.background = storage.getString("background", "")!!
		this.textColorInt = storage.getInt("color", 0)
		this.updateInterval = storage.getInt("updateInterval", 10)
		this.fontSize = storage.getInt("fontSize", 100)
		this.lastCacheTs = storage.getLong("lastCacheTs", 0L)
	}
}

data class UserData(private val context: Context) {
	private val storage = context.getSharedPreferences("userData", MODE_PRIVATE)
	var username: String = storage.getString("username", "")!!
	var password: String = storage.getString("password", "")!!
	var friends: Array<String> = storage.getStringSet("friends", emptySet<String>())!!.toTypedArray()
	var status: Status = Status(JSONObject(storage.getString("status", "{}")!!))
	fun save() {
		storage.edit()
			.putString("username", this.username)
			.putString("password", this.password)
			.putStringSet("friends", this.friends.toSet())
			.putString("status", this.status.json().toString())
			.apply()
	}

	fun load() {
		this.username = storage.getString("username", "")!!
		this.password = storage.getString("password", "")!!
		this.friends = storage.getStringSet("friends", emptySet<String>())!!.toTypedArray()
		this.status = Status(JSONObject(storage.getString("status", "{}")!!))
	}
}
