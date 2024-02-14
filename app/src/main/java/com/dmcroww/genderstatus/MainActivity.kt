package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.launch

class MainActivity: AppCompatActivity() {
	private var appData = AppOptions(applicationContext)
	private var storageManager: StorageManager = StorageManager(applicationContext)
	private var userData = UserData(applicationContext)

	// Define UI elements
	private lateinit var buttonUpdate: ImageButton
	private lateinit var buttonPreferences: ImageButton
	private val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.main)
		notificationManager.createNotificationChannel(NotificationChannel("friends_update_channel", "Friends Status Updates", NotificationManager.IMPORTANCE_LOW))
		val viewPager: ViewPager = findViewById(R.id.Pager)
		val adapter = SubScreenPagerAdapter(supportFragmentManager, userData.friends)
		viewPager.adapter = adapter
		// Initialize UI elements

		buttonUpdate = findViewById(R.id.button_update)
		buttonPreferences = findViewById(R.id.button_preferences)

		val serviceIntent = Intent(this, UpdateService::class.java)
		val updateIntent = Intent(this, UpdateSelf::class.java)
		val preferencesIntent = Intent(this, Preferences::class.java)

		startService(serviceIntent)
		buttonUpdate.setOnClickListener {startActivity(updateIntent)}
		buttonPreferences.setOnClickListener {startActivity(preferencesIntent)}

		sendBroadcast(Intent("com.dmcroww.genderstatus.FORCE_UPDATE"))

		Log.d("MAIN", "onCreate()")
		lifecycleScope.launch {
			if (appData.lastCacheTs < (System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) && userData.username.isNotBlank()) {
				val apiClient = ApiClient(applicationContext)

				apiClient.getArray("fetch backgrounds")
					.let {storageManager.fetchImage("backgrounds", it.toString(), true)}
				apiClient.getArray("fetch avatars")
					.let {storageManager.fetchImage("avatars", it.toString(), true)}

				appData.lastCacheTs = System.currentTimeMillis()
				appData.save()
			}
		}
	}

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onResume() {
		super.onResume()

		// Register broadcast receiver for data updates
		val filter = IntentFilter().apply {
			addAction("com.dmcroww.genderstatus.PREFERENCES_UPDATED")
			addAction("com.dmcroww.genderstatus.DATA_UPDATED")
			addAction("com.dmcroww.genderstatus.DATA_FAILED")
			addAction("com.dmcroww.genderstatus.DATA_NOT_FOUND")
		}

		registerReceiver(updateReceiver, filter)
		setTheme()
		Log.d("MAIN", "onResume()")
	}

	override fun onStop() {
		Log.d("MAIN", "onStop()")
		// Unregister the receiver in onStop
		unregisterReceiver(updateReceiver)
		super.onStop()
	}

	private inner class SubScreenPagerAdapter(fm: FragmentManager, private val usernames: Array<String>):
	 FragmentPagerAdapter(fm) {
		override fun getItem(position: Int): Fragment {
			// Create and return instance of SubScreenFragment with the username
			return SubScreenFragment.newInstance(usernames[position])
		}

		override fun getCount(): Int {
			// Return the number of sub-screens based on the number of usernames
			return usernames.size
		}
	}

	/**
	 * Reads stored partner status from SharedPreferences and updates the UI elements.
	 */
	@SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
	private fun readStoredStatus() {

		userData.friends.all {
			true
		}
	}


	/**
	 * Sets the app's theme based on user preferences.
	 */
	private fun setTheme() {
		appData = AppOptions(applicationContext)
		lifecycleScope.launch {
			val backgroundImage = if (appData.background != "") storageManager.fetchImage("backgrounds", appData.background) else null
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
