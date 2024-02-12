package com.dmcroww.genderstatus

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

class ApiClient(private val context: Context) {
	private val apiUrl: String = "https://api.dmcroww.tech/genderStatus/v2/"
	private val userData = UserData(context)

	var username: String = userData.username
	var password: String = userData.password
	/*
	DevMode: token "DevTest" used for testing purposes only,
					 dev usernames are "testUser" and "testPartner"

	TODO: add login logic where user receives the actual
				token on first launch after submitting correct
				username and password pair

	TODO: change API to accept username/password pair if
				token is "login" or smth smth
	*/

	suspend fun login(username: String, password: String) {
		withContext(Dispatchers.IO) {
			val response = makeCall(
				"login",
				JSONObject()
					.put("username", username)
					.put("password", password)
			)
			val success = response.optBoolean("success", false)

			userData.username = if (success) username else ""
			userData.password = if (success) password else ""
			userData.save()

			if (success) Log.i("API", "Login successful.")
			else Log.d("API", "Login failed. ERROR: " + response.optString("error", "undefined"))
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

	suspend fun getObject(action: String): JSONObject {
		return withContext(Dispatchers.IO) {
			val response = makeCall(action)
			if (response.optBoolean("success", false))
				response.getJSONObject("data")
			else JSONObject()
		}
	}

	suspend fun fetchFriendStatus(friendUsername: String): Status {
		return withContext(Dispatchers.IO) {
			val response = makeCall("fetch friend status", JSONObject().put("friend", friendUsername))
			if (response.optBoolean("success", false)) {
				Status(response.getJSONObject("data"))
			} else {
				Status(JSONObject())
			}
		}
	}

	suspend fun fetchFriendHistory(friendUsername: String): MutableList<Status> {
		return withContext(Dispatchers.IO) {
			val list = mutableListOf<Status>()
			val response = makeCall("fetch friend history", JSONObject().put("friend", friendUsername))
			if (response.optBoolean("success", false)) {
				val data = response.getJSONArray("data")
				for (i in 0 until data.length())
					list.plus(Status(data.getJSONObject(i)))
			}
			list
		}
	}

	suspend fun fetchFriends(): MutableMap<String, Status> {
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
			makeCall("post self", jsonObject)
		}
	}

	private suspend fun makeCall(action: String, data: JSONObject = JSONObject()): JSONObject {
		return withContext(Dispatchers.IO) {
			if (!data.has("username"))
				data.put("username", username)
			if (!data.has("password"))
				data.put("password", password)

			data.put("action", action)
			try {
				return@withContext suspendCancellableCoroutine {continuation ->
					Log.e("API call", data.toString())
					val jsonRequest = JsonObjectRequest(Request.Method.POST, apiUrl, data, {response -> continuation.resume(response)}, {error ->
						error.printStackTrace()
						Toast.makeText(context, "API ERR: Call failed. \nAction: $action", Toast.LENGTH_LONG).show()
						context.sendBroadcast(Intent("com.dmcroww.genderstatus.DATA_FAILED"))
						continuation.resume(JSONObject()) // Return an empty JSONObject in case of failure
					})
					val requestQueue = Volley.newRequestQueue(context.applicationContext)
					requestQueue.add(jsonRequest)
				}
			} catch (e: Exception) {
				// Handle exceptions if needed
				e.printStackTrace()
				JSONObject() // Return an empty JSONObject in case of failure
			}
		}
	}
}
