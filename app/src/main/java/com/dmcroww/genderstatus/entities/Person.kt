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
	var status: Status = Status(JSONObject())
) {
	fun save() {
		context.getSharedPreferences("people.$username", Context.MODE_PRIVATE)
			.edit()
			.putString("nickname", nickname)
			.putString("status", status.json().toString())
			.apply()
	}

	fun load() {
		val storage = context.getSharedPreferences("people.$username", Context.MODE_PRIVATE)
		this.nickname = storage.getString("nickname", "").toString()
		this.status = Status(JSONObject(storage.getString("status", "{}")!!))
	}
}
