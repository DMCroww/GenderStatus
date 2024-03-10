package com.dmcroww.genderstatus.entities

import android.content.Context
import android.content.res.Configuration
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.dmcroww.genderstatus.R
import com.dmcroww.genderstatus.providers.StorageManager

data class AppOptions(private val context: Context) {
	private val storage = context.getSharedPreferences("appData", Context.MODE_PRIVATE)
	var background: String
		get() = storage.getString("background", "")!!
		set(value) = storage.edit().putString("background", value).apply()
	var updateInterval: Int
		get() = storage.getInt("updateInterval", 10)
		set(value) = storage.edit().putInt("updateInterval", value).apply()
	var fontSize: Float
		get() = storage.getFloat("fontSize", 1.0f)
		set(value) = storage.edit().putFloat("fontSize", value).apply()
	var lastCacheTs: Long
		get() = storage.getLong("lastCacheTs", 0L)
		set(value) = storage.edit().putLong("lastCacheTs", value).apply()
	var darkMode: Int
		get() = storage.getInt("darkMode", 0) // Default value 0 for Follow System
		set(value) = storage.edit().putInt("darkMode", value).apply()
	var theme: Int
		get() = storage.getInt("theme", 0) // Default theme ID
		set(value) = storage.edit().putInt("theme", value).apply()

	suspend fun setBackground(layout: ConstraintLayout, backgroundImageElement: ImageView) {
		val defaultBackgroundIdx = if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 2 else 1

		// Use the default values if the selected values are 0 (indicating "Default")
		val backgroundArray = context.resources.obtainTypedArray(R.array.background_sources)
		layout.setBackgroundResource(backgroundArray.getResourceId(defaultBackgroundIdx, 0))
		backgroundArray.recycle()

		if (this.background.isNotBlank()) backgroundImageElement.setImageBitmap(StorageManager(context).fetchBackground(this.background))
	}
}