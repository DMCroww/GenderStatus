package com.dmcroww.genderstatus.entities

import android.content.Context
import org.json.JSONObject

data class UserData(private val context: Context) {
	private val storage = context.getSharedPreferences("userData", Context.MODE_PRIVATE)
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
