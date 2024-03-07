package com.dmcroww.genderstatus.entities

import android.content.Context
import org.json.JSONObject

data class UserData(private val context: Context) {
	private val storage = context.getSharedPreferences("userData", Context.MODE_PRIVATE)
	var username: String = storage.getString("username", "")!!
	var nickname: String = storage.getString("nickname", "")!!
	var password: String = storage.getString("password", "")!!
	var friends: Array<String> = storage.getStringSet("friends", emptySet<String>())!!.toTypedArray()
	var requests: Array<String> = storage.getStringSet("requests", emptySet<String>())!!.toTypedArray()
	var status: Status = Status(JSONObject(storage.getString("status", "{}")!!))
	fun save() {
		storage.edit()
			.putString("username", this.username)
			.putString("nickname", this.nickname)
			.putString("password", this.password)
			.putStringSet("friends", this.friends.toSet())
			.putStringSet("requests", this.requests.toSet())
			.putString("status", this.status.string())
			.apply()
	}

	fun load() {
		this.username = storage.getString("username", "")!!
		this.nickname = storage.getString("nickname", "")!!
		this.password = storage.getString("password", "")!!
		this.friends = storage.getStringSet("friends", emptySet<String>())!!.toTypedArray()
		this.requests = storage.getStringSet("requests", emptySet<String>())!!.toTypedArray()
		this.status = Status(JSONObject(storage.getString("status", "{}")!!))
	}
}
