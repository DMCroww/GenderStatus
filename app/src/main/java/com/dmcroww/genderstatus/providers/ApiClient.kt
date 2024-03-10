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

	fun login(username: String = this.username, password: String = this.password, onComplete: (Boolean, String) -> Unit) {
		if (username.isBlank() || password.isBlank()) return onComplete(false, "User not logged in, login credentials empty.") // Notify the caller with the result

		CoroutineScope(Dispatchers.IO).launch {
			val response = makeCall("login", JSONObject().put("username", username).put("password", password))
			val success = response.optBoolean("success", false)
			var errorMessage = ""
			if (success) {
				userData.username = username
				userData.password = password
				val friendsString = response.getJSONObject("data").getString("friends")
				val requestsString = response.getJSONObject("data").getString("requests")
				userData.friends = friendsString.split("|").filter {it.isNotBlank()}.toTypedArray()
				userData.requests = requestsString.split("|").filter {it.isNotBlank()}.toTypedArray()
			} else {
				errorMessage = response.optString("data", "undefined")
				userData.username = ""
				userData.password = ""
				Log.d("API", "Login failed. ERROR: $errorMessage")
			}
			withContext(Dispatchers.Main) {
				onComplete(success, errorMessage) // Notify the caller with the result
			}
		}
	}

	suspend fun getArray(action: String): JSONArray {
		return withContext(Dispatchers.IO) {
			val response = makeCall(action)
			if (response.optBoolean("success", false)) response.getJSONArray("data")
			else JSONArray()
		}
	}

	suspend fun fetchFriendHistory(friendUsername: String): List<Status> {
		return withContext(Dispatchers.IO) {
			val list = mutableListOf<Status>()
			val response = makeCall("get friend history", JSONObject().put("friend", friendUsername))
			if (response.optBoolean("success", false)) {
				val data = response.getJSONArray("data")
				for (i in 0 until data.length()) list.add(Status(data.getJSONObject(i)))
			}
			list
		}
	}

	suspend fun fetchFriends(): Map<String, JSONObject> {
		return withContext(Dispatchers.IO) {
			val list = mutableMapOf<String, JSONObject>()
			val response = makeCall("get friends")
			if (response.optBoolean("success", false)) {
				val data = response.getJSONArray("data")
				for (i in 0 until data.length()) {
					val obj = data.getJSONObject(i)
					val username = obj.getString("username")
					list[username] = obj
				}
			}
			list
		}
	}

	suspend fun postStatus(): JSONObject {
		return withContext(Dispatchers.IO) {
			makeCall("post status", JSONObject().put("status", userData.status.json()))
		}
	}

	suspend fun addFriend(username: String): JSONObject {
		return withContext(Dispatchers.IO) {
			makeCall("post friend add", JSONObject().put("friend", username))
		}
	}

	suspend fun removeFriend(username: String): JSONObject {
		return withContext(Dispatchers.IO) {
			makeCall("post friend remove", JSONObject().put("friend", username))
		}
	}

	private suspend fun makeCall(action: String, data: JSONObject = JSONObject()): JSONObject {
		return withContext(Dispatchers.IO) {
			if (!data.has("username")) data.put("username", username)
			if (!data.has("password")) data.put("password", password)

			data.put("action", action)
//			Log.d("API call", data.toString())

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
				while (reader.readLine().also {line = it} != null) responseStringBuilder.append(line).append("\n")

				reader.close()
				inputStream.close()
				val result = responseStringBuilder.toString()
//				Log.d("API response", result)

				return@withContext JSONObject(result)
			} catch (e: Exception) {
				// Handle exceptions if needed
				e.printStackTrace()
				return@withContext JSONObject().put("success", false).put("error", "Failed to make HTTP request: ${e.message}")
			} finally {
				connection.disconnect()
			}
		}
	}
}