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
import java.lang.reflect.Field
import kotlin.coroutines.resume

class ApiClient {

	companion object {
		private const val API_URL = "https://api.dmcroww.live/genderStatus/index.php"
		private const val API_TOKEN = "DevTest"

		/*
		DevMode: token "DevTest" used for testing purposes only,
		         dev usernames are "testUser" and "testPartner"

		TODO: add login logic where user receives the actual
		      token on first launch after submitting correct
		      username and password pair

		TODO: change API to accept username/password pair if
		      token is "login" or smth smth
    */

		suspend fun getData(context: Context, action: String, username: String? = AppOptions.getData(context).username): JSONArray {
			val jsonObject = JSONObject()
				.put("action", action)
			if (username != "") jsonObject.put("username", username)
			return withContext(Dispatchers.IO) {
				val result = makeCall(context, jsonObject)
				if (result.optBoolean("success", false)) {
					result.getJSONArray("data")
				} else {
					JSONArray()
				}
			}
		}

		private suspend fun fetchPerson(context: Context, partner: Boolean): Person {
			val appData = AppOptions.getData(context)
			val username = if (partner) appData.partner else appData.username
			val jsonObject = JSONObject()
				.put("action", if (partner) "fetch partner" else "fetch self")
				.put("username", username)
			return withContext(Dispatchers.IO) {
				val json = makeCall(context, jsonObject)

				val data = if (json.has("username")) json else null
				if (data === null) {
					Person("", "", "", "", 0, 0, 0, 0L)
				} else {
					Person(
						data.getString("username"),
						data.getString("avatar"),
						data.getString("activity"),
						data.getString("mood"),
						data.getInt("age"),
						data.getInt("sus"),
						data.getInt("gender"),
						data.getLong("timestamp")
					)
				}
			}
		}

		suspend fun fetchUser(context: Context): Person {
			return fetchPerson(context, false)
		}

		suspend fun fetchPartner(context: Context): Person {
			return fetchPerson(context, true)
		}

		suspend fun postData(context: Context, person: Person) {
			val jsonObject = JSONObject()
				.put("action", "post self")
				.put("username", person.username)

			val fields: Array<Field> = Person::class.java.declaredFields

			for (field in fields) {
				field.isAccessible = true
				val fieldName = field.name
				val fieldValue = field.get(person)
				jsonObject.put(fieldName, fieldValue)
			}
			withContext(Dispatchers.IO) {
				makeCall(context, jsonObject)
			}
		}

		private suspend fun makeCall(context: Context, jsonObject: JSONObject): JSONObject {
			return withContext(Dispatchers.IO) {
				jsonObject.put("token", API_TOKEN)
				try {
					return@withContext suspendCancellableCoroutine {continuation ->
						val jsonRequest = JsonObjectRequest(Request.Method.POST, API_URL, jsonObject, {response ->
							if (response.optBoolean("success", false))
								continuation.resume(response)
							else {
								val error = response.optString("error", "undefined")
								Toast.makeText(context, "API ERR: POST mismatch. \nResponse: $error", Toast.LENGTH_LONG).show()
							}
						}, {error ->
							error.printStackTrace()
							Log.e("API call", jsonObject.toString())
							val action = jsonObject.optString("action", "undefined")
							Toast.makeText(context, "API ERR: Call failed. \nAction: $action", Toast.LENGTH_LONG).show()
							context.sendBroadcast(Intent("com.dmcroww.genderstatus.DATA_FAILED"))
							// continuation.resume(JSONObject()) // Return an empty JSONObject in case of failure
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
}
