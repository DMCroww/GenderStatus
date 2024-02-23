package com.dmcroww.genderstatus.entities

import android.content.Context

data class AppOptions(private val context: Context) {
	private val storage = context.getSharedPreferences("appData", Context.MODE_PRIVATE)
	var background: String = storage.getString("background", "")!!
	var textColorInt: Int = storage.getInt("color", 0)
	var updateInterval: Int = storage.getInt("updateInterval", 10)
	var fontSize: Int = storage.getInt("fontSize", 100)
	var lastCacheTs: Long = storage.getLong("lastCacheTs", 0L)
	var debugToasts: Boolean = storage.getBoolean("debugToasts", false)

	fun save() {
		storage.edit()
			.putString("background", this.background)
			.putInt("color", this.textColorInt)
			.putInt("updateInterval", this.updateInterval)
			.putInt("fontSize", this.fontSize)
			.putLong("lastCacheTs", this.lastCacheTs)
			.putBoolean("debugToasts", this.debugToasts)
			.apply()
	}

	fun load() {
		this.background = storage.getString("background", "")!!
		this.textColorInt = storage.getInt("color", 0)
		this.updateInterval = storage.getInt("updateInterval", 10)
		this.fontSize = storage.getInt("fontSize", 100)
		this.lastCacheTs = storage.getLong("lastCacheTs", 0L)
		this.debugToasts = storage.getBoolean("debugToasts", false)
	}
}
