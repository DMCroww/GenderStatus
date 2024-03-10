@file:Suppress("DEPRECATION")

package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
	private lateinit var notificationManager: NotificationManager
	private lateinit var friendsList: Array<String>

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
		appData = AppOptions(this)
		storageManager = StorageManager(this)
		userData = UserData(this)

		notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannel(NotificationChannel("friends_update_channel", "Friends Status Updates", NotificationManager.IMPORTANCE_LOW))

		setContentView(R.layout.act_main)

		startService(Intent(this, UpdateService::class.java))
		findViewById<ImageButton>(R.id.button_update).setOnClickListener {
			startActivity(Intent(this, PostStatusActivity::class.java))
		}
		findViewById<ImageButton>(R.id.button_preferences).setOnClickListener {
			startActivity(Intent(this, PreferencesActivity::class.java))
		}

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
			}
		}
	}

	// Call this function whenever you want to refresh the ViewPager, such as in a broadcast receiver
	private fun refreshViewPager() {
		val viewPager: ViewPager = findViewById(R.id.Pager)
		friendsList = userData.friends
		val adapter = SubScreenPagerAdapter(supportFragmentManager, friendsList)
		viewPager.adapter = adapter
	}

	/**
	 * Sets the app's theme based on user preferences.
	 */
	private fun setTheme() {
		lifecycleScope.launch {
			appData.setBackground(findViewById(R.id.main_window), findViewById(R.id.backgroundImage))

			val nightMode = when (appData.darkMode) {
				1 -> AppCompatDelegate.MODE_NIGHT_YES
				2 -> AppCompatDelegate.MODE_NIGHT_NO
				else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
			}
			AppCompatDelegate.setDefaultNightMode(nightMode)
			when (appData.theme) {
				1 -> setTheme(R.style.AppTheme_Blue)
				2 -> setTheme(R.style.AppTheme_Pink)
				3 -> setTheme(R.style.AppTheme_Purple)
				4 -> setTheme(R.style.AppTheme_Magenta)
				5 -> setTheme(R.style.AppTheme_Red)
				6 -> setTheme(R.style.AppTheme_Orange)
				7 -> setTheme(R.style.AppTheme_Yellow)
				8 -> setTheme(R.style.AppTheme_Green)
				else -> setTheme(R.style.AppTheme) // Default theme
			}
		}
	}

	/**
	 * Broadcast receiver to handle updates and failures in data retrieval.
	 */
	private val updateReceiver = object: BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent?) {
			Log.d("Broadcast", intent?.action.toString())
			when (intent?.action) {
				"com.dmcroww.genderstatus.DATA_UPDATED" -> if (!(friendsList.sortedArray() contentEquals userData.friends.sortedArray())) refreshViewPager()
				"com.dmcroww.genderstatus.PREFERENCES_UPDATED" -> setTheme()
			}
		}
	}
}