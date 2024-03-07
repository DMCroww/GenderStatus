package com.dmcroww.genderstatus.entities

import android.content.Context
import android.content.res.Configuration
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.dmcroww.genderstatus.R
import com.dmcroww.genderstatus.providers.StorageManager

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

	suspend fun setBackground(layout: ConstraintLayout, backgroundImageElement: ImageView) {
		load()
		val defaultBackgroundIdx = if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 2 else 1

		// Use the default values if the selected values are 0 (indicating "Default")
		val backgroundArray = context.resources.obtainTypedArray(R.array.background_sources)
		layout.setBackgroundResource(backgroundArray.getResourceId(defaultBackgroundIdx, 0))
		backgroundArray.recycle()

		if (this.background.isNotBlank()) backgroundImageElement.setImageBitmap(StorageManager(context).fetchBackground(this.background))
	}

	fun getThemeColor(): Int {
		load()
		val defaultBackgroundIdx = if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 2 else 1

		val colorsArray = context.resources.obtainTypedArray(R.array.color_sources)
		val finalColorIdx = if (this.textColorInt == 0 || this.background == "") colorsArray.getColor(defaultBackgroundIdx, 0) else this.textColorInt

		colorsArray.recycle()
		return finalColorIdx
	}
}
