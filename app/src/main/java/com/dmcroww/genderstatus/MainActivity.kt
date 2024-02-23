@file:Suppress("DEPRECATION")

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
import com.dmcroww.genderstatus.entities.AppOptions
import com.dmcroww.genderstatus.entities.UserData
import com.dmcroww.genderstatus.providers.ApiClient
import com.dmcroww.genderstatus.providers.StorageManager
import com.dmcroww.genderstatus.providers.SubScreenFragment
import com.dmcroww.genderstatus.providers.UpdateService
import kotlinx.coroutines.launch

class MainActivity: AppCompatActivity() {
	private lateinit var appData: AppOptions
	private lateinit var storageManager: StorageManager
	private lateinit var userData: UserData
	private lateinit var apiClient: ApiClient

	// Define UI elements
	private lateinit var buttonPostStatus: ImageButton
	private lateinit var buttonPreferences: ImageButton
	private lateinit var notificationManager: NotificationManager

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		apiClient = ApiClient(applicationContext)

		apiClient.login {success, errorMessage ->
			if (!success) {
				// Login failed, present user with login screen
				Log.e("MAIN", "Error: $errorMessage")
				startActivity(Intent(this, LoginActivity::class.java))
				finish()
			}
		}
		continueSetup()
		Log.d("MAIN", "onCreate()")
	}

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onResume() {
		super.onResume()
		Log.d("MAIN", "onResume()")

		refreshViewPager()

		// Register broadcast receiver for data updates
		val filter = IntentFilter().apply {
			addAction("com.dmcroww.genderstatus.PREFERENCES_UPDATED")
			addAction("com.dmcroww.genderstatus.DATA_UPDATED")
			addAction("com.dmcroww.genderstatus.DATA_FAILED")
			addAction("com.dmcroww.genderstatus.DATA_NOT_FOUND")
		}

		registerReceiver(updateReceiver, filter)
		setTheme()
	}

	override fun onStop() {
		Log.d("MAIN", "onStop()")
		// Unregister the receiver in onStop
		unregisterReceiver(updateReceiver)
		super.onStop()
	}

	private inner class SubScreenPagerAdapter(fm: FragmentManager, private val usernames: Array<String>): FragmentPagerAdapter(fm) {
		override fun getItem(position: Int): Fragment {
			return SubScreenFragment.newInstance(usernames[position])
		}

		override fun getCount(): Int {
			return usernames.size
		}
	}

	private fun continueSetup() {
		appData = AppOptions(applicationContext)
		storageManager = StorageManager(applicationContext)
		userData = UserData(applicationContext)

		notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannel(NotificationChannel("friends_update_channel", "Friends Status Updates", NotificationManager.IMPORTANCE_LOW))

		setContentView(R.layout.act_main)

		buttonPostStatus = findViewById(R.id.button_update)
		buttonPreferences = findViewById(R.id.button_preferences)

		startService(Intent(this, UpdateService::class.java))
		buttonPostStatus.setOnClickListener {startActivity(Intent(this, PostStatusActivity::class.java))}
		buttonPreferences.setOnClickListener {startActivity(Intent(this, PreferencesActivity::class.java))}

		sendBroadcast(Intent("com.dmcroww.genderstatus.FORCE_UPDATE"))

		lifecycleScope.launch {
			if (appData.lastCacheTs < (System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) && userData.username.isNotBlank()) {

				val backgroundArray = apiClient.getArray("fetch backgrounds")
				for (i in 0 until backgroundArray.length()) {
					val filename = backgroundArray.optString(i)
					storageManager.fetchBackground(filename, true)
				}
				val avatarArray = apiClient.getArray("fetch avatars")
				for (i in 0 until avatarArray.length()) {
					val filename = avatarArray.optString(i)
					storageManager.fetchAvatar(filename, true)
				}

				appData.lastCacheTs = System.currentTimeMillis()
				appData.save()
			}
		}
	}

	// Call this function whenever you want to refresh the ViewPager, such as in a broadcast receiver
	private fun refreshViewPager() {
		val viewPager: ViewPager = findViewById(R.id.Pager)
		val adapter = SubScreenPagerAdapter(supportFragmentManager, userData.friends)
		viewPager.adapter = adapter
	}

	/**
	 * Sets the app's theme based on user preferences.
	 */
	private fun setTheme() {
		appData.load()
		if (appData.background.isNotBlank())
			lifecycleScope.launch {
				findViewById<ImageView>(R.id.backgroundImage).setImageBitmap(storageManager.fetchBackground(appData.background))
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

		buttonPostStatus.setColorFilter(finalColorIdx)
		buttonPreferences.setColorFilter(finalColorIdx)
	}

	/**
	 * Broadcast receiver to handle updates and failures in data retrieval.
	 */
	private val updateReceiver = object: BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent?) {
			Log.d("Broadcast", intent?.action.toString())
			when (intent?.action) {
				"com.dmcroww.genderstatus.DATA_UPDATED" -> {
					refreshViewPager()
				}

				"com.dmcroww.genderstatus.PREFERENCES_UPDATED" -> {
					setTheme()
				}
			}
		}
	}
}
