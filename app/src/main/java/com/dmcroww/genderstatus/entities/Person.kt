package com.dmcroww.genderstatus.entities

import android.content.Context
import org.json.JSONObject

/**
 * Person class to manage easier saving and loading.
 */
data class Person(
	private val context: Context,
	var username: String,
	var nickname: String = "",
	var displayName: String = "",
	var status: Status = Status(JSONObject()),
	var timestamp: Long = 0L
) {
	fun save() {
		context.getSharedPreferences("people.$username", Context.MODE_PRIVATE).edit()
			.putString("nickname", this.nickname)
			.putString("displayName", this.displayName)
			.putString("status", this.status.string())
			.putLong("timestamp", this.status.timestamp)
			.apply()
	}

	fun load() {
		val storage = context.getSharedPreferences("people.$username", Context.MODE_PRIVATE)
		this.nickname = storage.getString("nickname", nickname).toString()
		this.displayName = storage.getString("displayName", displayName).toString()
		this.status = Status(JSONObject(storage.getString("status", "{}")!!))
		this.timestamp = this.status.timestamp
	}
}
