package com.dmcroww.genderstatus.entities

import android.content.Context
import org.json.JSONObject

data class UserData(private val context: Context) {
	private val storage = context.getSharedPreferences("userData", Context.MODE_PRIVATE)
	var username: String
		get() = storage.getString("username", "")!!
		set(value) = storage.edit().putString("username", value).apply()
	var nickname: String
		get() = storage.getString("nickname", "")!!
		set(value) = storage.edit().putString("nickname", value).apply()
	var password: String
		get() = storage.getString("password", "")!!
		set(value) = storage.edit().putString("password", value).apply()
	var friends: Array<String>
		get() = storage.getStringSet("friends", emptySet<String>())!!.toTypedArray()
		set(value) = storage.edit().putStringSet("friends", value.toSet()).apply()
	var requests: Array<String>
		get() = storage.getStringSet("requests", emptySet<String>())!!.toTypedArray()
		set(value) = storage.edit().putStringSet("requests", value.toSet()).apply()
	var status: Status
		get() = Status(JSONObject(storage.getString("status", "{}")!!))
		set(value) = storage.edit().putString("password", value.string()).apply()
}