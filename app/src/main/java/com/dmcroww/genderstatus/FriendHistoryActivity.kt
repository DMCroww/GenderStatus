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
import androidx.lifecycle.lifecycleScope
import com.dmcroww.genderstatus.entities.AppOptions
import com.dmcroww.genderstatus.providers.ApiClient
import com.dmcroww.genderstatus.providers.StorageManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FriendHistoryActivity: AppCompatActivity() {
	private lateinit var storageManager: StorageManager
	private lateinit var appData: AppOptions
	private lateinit var apiClient: ApiClient
	private lateinit var historyDataLayout: LinearLayout

	private var finalColorIdx: Int = 0
	private val rotateAnimation = RotateAnimation(
		0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
	).apply {
		duration = 2000 // Set the duration of the rotation
		repeatCount = -1 // Make the rotation continuous
	}

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
		storageManager = StorageManager(applicationContext)
		appData = AppOptions(applicationContext)
		apiClient = ApiClient(applicationContext)
		setContentView(R.layout.act_friend_history)

		// Register the receiver for the broadcast
		val filter = IntentFilter("com.dmcroww.genderstatus.DATA_UPDATED")
		registerReceiver(dataUpdateReceiver, filter)

		initializeUI()
		refreshHistory()
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun initializeUI() {
		loading()
		historyDataLayout = findViewById(R.id.history_data)

		val backgroundArray = resources.obtainTypedArray(R.array.background_sources)
		val colorsArray = resources.obtainTypedArray(R.array.color_sources)

		// Determine default values based on system theme
		val defaultBackgroundIdx = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 2 else 1


		finalColorIdx = if (appData.textColorInt == 0 || appData.background == "") colorsArray.getColor(defaultBackgroundIdx, 0) else appData.textColorInt
		findViewById<TextView>(R.id.title_history).setTextColor(finalColorIdx)

		// Use the default values if the selected values are 0 (indicating "Default")
		findViewById<ConstraintLayout>(R.id.main).setBackgroundResource(backgroundArray.getResourceId(defaultBackgroundIdx, 0))

		GlobalScope.launch(Dispatchers.IO) {
			val backgroundImage = if (appData.background != "") storageManager.fetchBackground(appData.background) else null
			runOnUiThread {
				findViewById<ImageView>(R.id.backgroundImage).setImageBitmap(backgroundImage)
			}
		}

		backgroundArray.recycle()
		colorsArray.recycle()
	}

	@SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
	private fun refreshHistory() {
		historyDataLayout.removeAllViews()

		lifecycleScope.launch {
			try {
				val history = apiClient.fetchFriendHistory(intent.getStringExtra("username")!!)

				// Check if the stored string is not null
				if (history.isEmpty()) {
					Toast.makeText(applicationContext, "Friend history empty!", Toast.LENGTH_SHORT).show()
					loaded()
				} else {
					// Inflate the template layout for each entry

					val genders = resources.getStringArray(R.array.genders_array)
					val ages = resources.getStringArray(R.array.ages_array)
					val bigSize = 20.0f * (appData.fontSize / 100.0f)
					val medSize = 20.0f * (appData.fontSize / 100.0f)
					val smallSize = 18.0f * (appData.fontSize / 100.0f)
					val tinySize = 16.0f * (appData.fontSize / 100.0f)

					for (i in history.indices) {
						val status = history.get(i)

						val entryView = LayoutInflater.from(applicationContext).inflate(R.layout.frag_friend_history_entry, historyDataLayout, false)

						// Populate the entryView with data from historyObject

						val image = storageManager.fetchAvatar(status.avatar)
						if (image != null) entryView.findViewById<ImageView>(R.id.avatar).setImageBitmap(image)
						else entryView.findViewById<ImageView>(R.id.avatar).setImageDrawable(getDrawable(R.drawable.android_128))

						val dataActivity = entryView.findViewById<TextView>(R.id.activity)
						dataActivity.text = status.activity
						dataActivity.setTextColor(finalColorIdx)
						dataActivity.textSize = bigSize

						val dataMood = entryView.findViewById<TextView>(R.id.mood)
						dataMood.text = status.mood
						dataMood.setTextColor(finalColorIdx)
						dataMood.textSize = medSize

						val dataDate = entryView.findViewById<TextView>(R.id.date)
						dataDate.text = timestampToRelativePlus(status.timestamp)
						dataDate.setTextColor(finalColorIdx)
						dataDate.textSize = tinySize

						val dataOther = entryView.findViewById<TextView>(R.id.other)
						dataOther.text = "${genders[status.gender]} ${ages[status.age]}, ${if (status.sus > 0) "${(status.sus - 1) * 10}%sus" else "..."}"
						dataOther.setTextColor(finalColorIdx)
						dataOther.textSize = smallSize

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
