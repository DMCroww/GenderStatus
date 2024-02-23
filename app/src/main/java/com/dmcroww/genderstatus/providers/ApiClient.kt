package com.dmcroww.genderstatus.providers

import android.content.Context
import android.util.Log
import com.dmcroww.genderstatus.entities.Status
import com.dmcroww.genderstatus.entities.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(context: Context) {
	private val apiUrl: String = "https://api.dmcroww.tech/genderStatus/v2/"
	private val userData = UserData(context)

	private var username: String = userData.username
	private var password: String = userData.password

	fun login(
		username: String = this.username,
		password: String = this.password,
		onComplete: (Boolean, String) -> Unit // Callback to handle login result
	) {
		if (username.isBlank() || password.isBlank())
			return onComplete(false, "User not logged in, login credentials empty.") // Notify the caller with the result

		CoroutineScope(Dispatchers.IO).launch {
			val response = makeCall(
				"login",
				JSONObject()
					.put("username", username)
					.put("password", password)
			)
			val success = response.optBoolean("success", false)
			var errorMessage = ""
			if (success) {
				userData.username = username
				userData.password = password

				val friendList = mutableListOf<String>()
				val friendsJSON = response.getJSONObject("data").getJSONArray("friends")

				for (i in 0 until friendsJSON.length()) {
					val element = friendsJSON.optString(i)
					friendList.add(element)
				}
				userData.friends = friendList.toTypedArray()
				Log.i("API", "Login successful.")
			} else {
				errorMessage = response.optString("error", "undefined")

				userData.username = ""
				userData.password = ""
				Log.d("API", "Login failed. ERROR: $errorMessage")
			}

			userData.save()

			withContext(Dispatchers.Main) {
				onComplete(success, errorMessage) // Notify the caller with the result
			}

		}
	}

	suspend fun getArray(action: String): JSONArray {
		return withContext(Dispatchers.IO) {
			val response = makeCall(action)
			if (response.optBoolean("success", false))
				response.getJSONArray("data")
			else JSONArray()
		}
	}

// TODO: might not be needed after all?
//	suspend fun getObject(action: String): JSONObject {
//		return withContext(Dispatchers.IO) {
//			val response = makeCall(action)
//			if (response.optBoolean("success", false))
//				response.getJSONObject("data")
//			else JSONObject()
//		}
//	}

	suspend fun fetchFriendHistory(friendUsername: String): List<Status> {
		return withContext(Dispatchers.IO) {
			val list = mutableListOf<Status>()
			val response = makeCall("fetch friend history", JSONObject().put("friend", friendUsername))
			if (response.optBoolean("success", false)) {

				val data = response.getJSONArray("data")
				Log.d("API", data.toString())
				for (i in 0 until data.length())
					list.add(Status(data.getJSONObject(i)))
			}
			list
		}
	}

	suspend fun fetchFriends(): Map<String, Status> {
		return withContext(Dispatchers.IO) {
			val list = mutableMapOf<String, Status>()
			val response = makeCall("fetch friends")
			if (response.optBoolean("success", false)) {
				val data = response.getJSONArray("data")
				for (i in 0 until data.length()) {
					val obj = data.getJSONObject(i)
					val username = obj.getString("username")
					list[username] = Status(obj)
				}
			}
			list
		}
	}

	suspend fun postStatus() {
		userData.load()
		withContext(Dispatchers.IO) {
			val jsonObject = JSONObject()
			for (field in Status::class.java.declaredFields) {
				field.isAccessible = true
				jsonObject.put(field.name, field.get(userData.status))
			}
			Log.d("API", jsonObject.toString())
			makeCall("post status", jsonObject)
		}
	}

	private suspend fun makeCall(action: String, data: JSONObject = JSONObject()): JSONObject {
		return withContext(Dispatchers.IO) {
			if (!data.has("username"))
				data.put("username", username)
			if (!data.has("password"))
				data.put("password", password)

			data.put("action", action)
			Log.d("API", data.toString())

			val url = URL(apiUrl)
			val connection = url.openConnection() as HttpURLConnection
			connection.requestMethod = "POST"
			connection.setRequestProperty("Content-Type", "application/json")
			connection.doOutput = true

			try {
				// Write the data to the request body
				val outputStream = connection.outputStream
				val writer = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))
				writer.write(data.toString())
				writer.flush()
				writer.close()
				outputStream.close()

				// Read the response
				val inputStream = connection.inputStream
				val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
				val responseStringBuilder = StringBuilder()
				var line: String?
				while (reader.readLine().also {line = it} != null) {
					responseStringBuilder.append(line).append("\n")
				}
				reader.close()
				inputStream.close()

				return@withContext JSONObject(responseStringBuilder.toString())
			} catch (e: Exception) {
				// Handle exceptions if needed
				e.printStackTrace()
				val errorJson = JSONObject()
				errorJson.put("success", false)
				errorJson.put("error", "Failed to make HTTP request: ${e.message}")
				return@withContext errorJson
			} finally {
				connection.disconnect()
			}
		}
	}
}
