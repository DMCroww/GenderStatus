package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity: AppCompatActivity() {

	// Define UI elements
	private lateinit var dataActivity: TextView
	private lateinit var dataMood: TextView
	private lateinit var dataAge: TextView
	private lateinit var dataSus: TextView
	private lateinit var dataGender: TextView
	private lateinit var dataDelta: TextView
	private lateinit var dataUpdated: TextView
	private lateinit var buttonUpdate: ImageButton
	private lateinit var buttonPreferences: ImageButton
	private lateinit var buttonHistory: ImageButton

	private lateinit var appData: AppOptions

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.main)

		appData = AppOptions.getData(applicationContext)

		// Initialize UI elements
		dataActivity = findViewById(R.id.dataActivity)
		dataMood = findViewById(R.id.dataMood)
		dataAge = findViewById(R.id.dataAge)
		dataSus = findViewById(R.id.dataSus)
		dataGender = findViewById(R.id.dataGender)
		dataDelta = findViewById(R.id.dataDelta)
		dataUpdated = findViewById(R.id.dataUpdated)
		buttonUpdate = findViewById(R.id.button_update)
		buttonPreferences = findViewById(R.id.button_preferences)
		buttonHistory = findViewById(R.id.button_history)

		val serviceIntent = Intent(this, UpdateService::class.java)
		val updateIntent = Intent(this, UpdateSelf::class.java)
		val preferencesIntent = Intent(this, Preferences::class.java)
		val historyIntent = Intent(this, PartnerHistory::class.java)

		startService(serviceIntent)
		buttonUpdate.setOnClickListener {startActivity(updateIntent)}
		buttonPreferences.setOnClickListener {startActivity(preferencesIntent)}
		buttonHistory.setOnClickListener {startActivity(historyIntent)}

		sendBroadcast(Intent("com.dmcroww.genderstatus.FORCE_UPDATE"))

		Log.d("MAIN", "onCreate()")
	}

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onResume() {
		super.onResume()

		// Register broadcast receiver for data updates
		val filter = IntentFilter()
		filter.addAction("com.dmcroww.genderstatus.PREFERENCES_UPDATED")
		filter.addAction("com.dmcroww.genderstatus.DATA_UPDATED")
		filter.addAction("com.dmcroww.genderstatus.DATA_FAILED")
		filter.addAction("com.dmcroww.genderstatus.DATA_NOT_FOUND")

		registerReceiver(updateReceiver, filter)
		// Read stored status, set theme, and set up intents
		readStoredStatus()
		Log.d("MAIN", "onResume()")
	}

	override fun onStop() {
		Log.d("MAIN", "onStop()")
		// Unregister the receiver in onStop
		unregisterReceiver(updateReceiver)
		super.onStop()
	}

	/**
	 * Reads stored partner status from SharedPreferences and updates the UI elements.
	 */
	@SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
	private fun readStoredStatus() {
		val genders: Array<String> = resources.getStringArray(R.array.genders_array)
		val ages: Array<String> = resources.getStringArray(R.array.ages_array)

		val partner = StorageManager.getPartner(applicationContext)

		val delta = timestampToDelta(partner.timestamp)
		val updated = timestampToDateTime(partner.timestamp)

		setTheme()
		lifecycleScope.launch {

			val image = StorageManager.fetchImage(applicationContext, "avatars", partner.avatar)
			if (image != null) {
				findViewById<ImageView>(R.id.avatar).setImageBitmap(image)
			} else {
				findViewById<ImageView>(R.id.avatar).setImageDrawable(getDrawable(R.drawable.android_128))
			}
			if (appData.lastCacheTs < (System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) && appData.username != "") {
				val backgrounds = ApiClient.getData(applicationContext, "backgrounds")
				(0 until backgrounds.length()).map {
					StorageManager.fetchImage(applicationContext, "backgrounds", backgrounds.getString(it), true)
				}
				val avatars = ApiClient.getData(applicationContext, "avatars")
				(0 until avatars.length()).map {
					StorageManager.fetchImage(applicationContext, "avatars", avatars.getString(it), true)
				}
				appData.lastCacheTs = System.currentTimeMillis()
				AppOptions.saveData(applicationContext, appData)
			}

		}

		dataActivity.text = partner.activity
		dataMood.text = partner.mood
		dataAge.text = ages[partner.age]
		dataSus.text = if (partner.sus > 0) "${(partner.sus - 1) * 10}%" else "..."
		dataGender.text = genders[partner.gender]
		dataDelta.text = delta
		dataUpdated.text = "($updated)"
	}

	/**
	 * Converts a Unix timestamp to a formatted date and time string.
	 */
	private fun timestampToDateTime(unixTimestamp: Long): String {
		return if (unixTimestamp > 0) SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault()).format(Date(unixTimestamp * 1000L))
		else "..."
	}

	/**
	 * Converts a Unix timestamp to a time difference to current time.
	 */
	private fun timestampToDelta(unixTimestamp: Long): String {
		return if (unixTimestamp > 0) DateUtils.getRelativeTimeSpanString(unixTimestamp * 1000L, System.currentTimeMillis(), 60000L).toString()
		else "..."
	}

	/**
	 * Sets the app's theme based on user preferences.
	 */
	private fun setTheme() {
		appData = AppOptions.getData(applicationContext)
		lifecycleScope.launch {
			val backgroundImage = if (appData.background != "") StorageManager.fetchImage(applicationContext, "backgrounds", appData.background) else null
			findViewById<ImageView>(R.id.backgroundImage).setImageBitmap(backgroundImage)
		}

		// Determine default values based on system theme
		val defaultBackgroundIdx = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 2 else 1

		// Use the default values if the selected values are 0 (indicating "Default")
		val backgroundArray = resources.obtainTypedArray(R.array.background_sources)
		findViewById<ConstraintLayout>(R.id.main_window).setBackgroundResource(backgroundArray.getResourceId(defaultBackgroundIdx, 0))

		val colorsArray = resources.obtainTypedArray(R.array.color_sources)
		val finalColorIdx = if (appData.textColorInt == 0 || appData.background == "") colorsArray.getColor(defaultBackgroundIdx, 0) else appData.textColorInt

		backgroundArray.recycle()
		colorsArray.recycle()

		val titleActivity = findViewById<TextView>(R.id.title_activity)
		val titleMood = findViewById<TextView>(R.id.title_mood)
		val titleAge = findViewById<TextView>(R.id.title_age)
		val titleSus = findViewById<TextView>(R.id.title_sus)
		val titleGender = findViewById<TextView>(R.id.title_gender)
		val titleUpdated = findViewById<TextView>(R.id.title_updated)

		val bigSize = 32.0f * (appData.fontSize / 100.0f)
		val medSize = 24.0f * (appData.fontSize / 100.0f)
		val smallSize = 20.0f * (appData.fontSize / 100.0f)
		val tinySize = 18.0f * (appData.fontSize / 100.0f)

		titleActivity.setTextColor(finalColorIdx)
		titleMood.setTextColor(finalColorIdx)
		titleAge.setTextColor(finalColorIdx)
		titleSus.setTextColor(finalColorIdx)
		titleGender.setTextColor(finalColorIdx)
		titleUpdated.setTextColor(finalColorIdx)
		dataActivity.setTextColor(finalColorIdx)
		dataMood.setTextColor(finalColorIdx)
		dataAge.setTextColor(finalColorIdx)
		dataSus.setTextColor(finalColorIdx)
		dataGender.setTextColor(finalColorIdx)
		dataDelta.setTextColor(finalColorIdx)
		dataUpdated.setTextColor(finalColorIdx)

		titleActivity.textSize = bigSize
		titleMood.textSize = bigSize

		dataActivity.textSize = medSize
		dataMood.textSize = medSize

		titleAge.textSize = smallSize
		titleSus.textSize = smallSize
		titleGender.textSize = smallSize
		dataAge.textSize = smallSize
		dataSus.textSize = smallSize
		dataGender.textSize = smallSize

		titleUpdated.textSize = smallSize
		dataDelta.textSize = smallSize
		dataUpdated.textSize = tinySize

		buttonHistory.setColorFilter(finalColorIdx)
		buttonUpdate.setColorFilter(finalColorIdx)
		buttonPreferences.setColorFilter(finalColorIdx)
	}

	/**
	 * Broadcast receiver to handle updates and failures in data retrieval.
	 */
	private val updateReceiver = object: BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent?) {
			Log.d("Broadcast", intent.toString())
			when (intent?.action) {
				"com.dmcroww.genderstatus.DATA_UPDATED" -> {
					readStoredStatus()
				}

				"com.dmcroww.genderstatus.PREFERENCES_UPDATED" -> {
					setTheme()
				}

				"com.dmcroww.genderstatus.DATA_FAILED" -> {
					startActivity(Intent(context, Preferences::class.java))
					Toast.makeText(applicationContext, "Usernames empty or invalid.", Toast.LENGTH_LONG).show()
				}

				"com.dmcroww.genderstatus.DATA_NOT_FOUND" -> {
					startActivity(Intent(context, Preferences::class.java))
					Toast.makeText(applicationContext, "One of usernames doesn't exist!", Toast.LENGTH_LONG).show()
				}
			}
		}
	}
}
