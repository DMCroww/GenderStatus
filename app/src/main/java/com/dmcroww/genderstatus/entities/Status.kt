package com.dmcroww.genderstatus.entities

import org.json.JSONObject

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
