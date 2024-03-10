package com.dmcroww.genderstatus.entities

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Person class to manage saving and loading.
 */
data class Person(val context: Context, val username: String) {
	private val storage: SharedPreferences = context.getSharedPreferences("people.$username", Context.MODE_PRIVATE)
	var nickname: String
		get() = storage.getString("nickname", "").toString()
		set(value) = storage.edit().putString("nickname", value).apply()
	var displayName: String
		get() = storage.getString("displayName", "").toString()
		set(value) = storage.edit().putString("DisplayName", value).apply()
	var status: Status
		get() = Status(JSONObject(storage.getString("status", "{}")!!))
		set(value) = storage.edit().putString("status", value.string()).apply()
	var timestamp: Long
		get() = this.status.timestamp
		set(value) = storage.edit().putLong("timestamp", value).apply()
}

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

	fun string(): String {
		return this.json().toString()
	}
}