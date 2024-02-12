package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private lateinit var appData: AppOptions
private var finalColorIdx: Int = 0
private lateinit var rotateAnimation: RotateAnimation

class FriendHistory: AppCompatActivity() {

	private val dataUpdateReceiver = object: BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			// Handle data update here, you can refresh the history
			loading(true)
			refreshHistory()
		}
	}

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.friend_history)

		// Register the receiver for the broadcast
		val filter = IntentFilter("com.dmcroww.genderstatus.DATA_UPDATED")
		registerReceiver(dataUpdateReceiver, filter)

		initializeUI()
		refreshHistory()
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun initializeUI() {
		rotateAnimation = RotateAnimation(
			0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
		)
		rotateAnimation.duration = 2000 // Set the duration of the rotation
		rotateAnimation.repeatCount = -1 // Make the rotation continuous
		loading()

		appData = AppOptions.getData(applicationContext)

		// Determine default values based on system theme
		val defaultBackgroundIdx = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 2 else 1

		// Use the default values if the selected values are 0 (indicating "Default")
		val backgroundArray = resources.obtainTypedArray(R.array.background_sources)
		findViewById<ConstraintLayout>(R.id.main).setBackgroundResource(backgroundArray.getResourceId(defaultBackgroundIdx, 0))

		GlobalScope.launch(Dispatchers.IO) {
			val backgroundImage = if (appData.background != "") StorageManager.fetchImage(applicationContext, "backgrounds", appData.background) else null
			runOnUiThread {
				findViewById<ImageView>(R.id.backgroundImage).setImageBitmap(backgroundImage)
			}
		}

		val colorsArray = resources.obtainTypedArray(R.array.color_sources)
		finalColorIdx = if (appData.textColorInt == 0 || appData.background == "") colorsArray.getColor(defaultBackgroundIdx, 0) else appData.textColorInt

		findViewById<TextView>(R.id.title_history).setTextColor(finalColorIdx)
		backgroundArray.recycle()
		colorsArray.recycle()
	}

	@OptIn(DelicateCoroutinesApi::class)
	@SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
	private fun refreshHistory() {
		GlobalScope.launch(Dispatchers.IO) {
			val genders: Array<String> = resources.getStringArray(R.array.genders_array)
			val ages: Array<String> = resources.getStringArray(R.array.ages_array)
			val historyDataLayout = findViewById<LinearLayout>(R.id.history_data)
			historyDataLayout.removeAllViews()

			try {
				val history = StorageManager.getPartnerHistory(applicationContext)

				val bigSize = 20.0f * (appData.fontSize / 100.0f)
				val medSize = 20.0f * (appData.fontSize / 100.0f)
				val smallSize = 18.0f * (appData.fontSize / 100.0f)
				val tinySize = 16.0f * (appData.fontSize / 100.0f)

				historyDataLayout.removeAllViews()

				// Check if the stored string is not null
				if (history.length() > 0) {
					// Inflate the template layout for each entry
					for (i in 0 until history.length()) {
						val historyObject = history.getJSONObject(i)

						val entryView = LayoutInflater.from(applicationContext).inflate(R.layout.history_entry, historyDataLayout, false)

						val timestamp = historyObject.optLong("timestamp", 0)
						val avatar = historyObject.optString("avatar", "...")
						val activity = historyObject.optString("activity", "...")
						val mood = historyObject.optString("mood", "...")
						val gender = historyObject.optInt("gender", 0)
						val age = historyObject.optInt("age", 0)
						val sus = historyObject.optInt("sus", 0)

						// Populate the entryView with data from historyObject
						val dataAvatar = entryView.findViewById<ImageView>(R.id.avatar)
						val dataDate = entryView.findViewById<TextView>(R.id.date)
						val dataActivity = entryView.findViewById<TextView>(R.id.activity)
						val dataMood = entryView.findViewById<TextView>(R.id.mood)
						val dataOther = entryView.findViewById<TextView>(R.id.other)

						val image = StorageManager.fetchImage(applicationContext, "avatars", avatar.toString())
						if (image != null) {
							dataAvatar.setImageBitmap(image)
						} else {
							dataAvatar.setImageDrawable(getDrawable(R.drawable.android_128))
						}

						dataActivity.text = activity
						dataActivity.setTextColor(finalColorIdx)
						dataActivity.textSize = bigSize

						dataMood.text = mood
						dataMood.setTextColor(finalColorIdx)
						dataMood.textSize = medSize

						dataOther.text = "${genders[gender]} ${ages[age]}, ${(sus - 1) * 10}% sus"
						dataOther.setTextColor(finalColorIdx)
						dataOther.textSize = smallSize

						dataDate.text = timestampToRelativePlus(timestamp)
						dataDate.setTextColor(finalColorIdx)
						dataDate.textSize = tinySize

						runOnUiThread {
							historyDataLayout.addView(entryView)
						}
					}
					// Add empty Space with set height (16dp in this example)
					val space = Space(applicationContext)
					space.layoutParams = LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, 600 // Adjust height as needed
					)

					// Add the Space to your LinearLayout
					runOnUiThread {
						historyDataLayout.addView(space)
					}
					loaded()
				} else {
					Toast.makeText(applicationContext, "Partner history empty!", Toast.LENGTH_LONG).show()
					loaded()
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	private fun loaded() {
		runOnUiThread {
			val historyDataLayout = findViewById<LinearLayout>(R.id.history_data)
			val fadeAnimation = AlphaAnimation(historyDataLayout.alpha, 1F)
			fadeAnimation.duration = 250
			fadeAnimation.fillAfter = true
			historyDataLayout.startAnimation(fadeAnimation)
			findViewById<ImageView>(R.id.loadingImage).clearAnimation()
			findViewById<ImageView>(R.id.loadingImage).visibility = View.GONE
		}
	}

	private fun loading(animateOut: Boolean = false) {
		runOnUiThread {
			if (animateOut) {
				val historyDataLayout = findViewById<LinearLayout>(R.id.history_data)
				val fadeAnimation = AlphaAnimation(1F, 0.25F)
				fadeAnimation.duration = 250
				fadeAnimation.fillAfter = true
				historyDataLayout.startAnimation(fadeAnimation)
			}
			findViewById<ImageView>(R.id.loadingImage).startAnimation(rotateAnimation)
			findViewById<ImageView>(R.id.loadingImage).visibility = View.VISIBLE
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		// Unregister the receiver when the activity is destroyed
		unregisterReceiver(dataUpdateReceiver)
	}

	private fun timestampToRelativePlus(unixTimestamp: Long): String {
		val timestamp = unixTimestamp * 1000L
		val currentTimeMillis = System.currentTimeMillis()
		val relative = DateUtils.getRelativeTimeSpanString(timestamp, currentTimeMillis, DateUtils.MINUTE_IN_MILLIS).toString()

		// If more than one day, show the date and time
		val dateFormat = SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault())
		val date = Date(timestamp)
		val formattedDate = dateFormat.format(date)
		return "$relative ($formattedDate)"
	}
}
