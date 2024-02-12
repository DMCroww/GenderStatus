package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class UpdateService: Service() {

	private var timer = Timer()
	private var isServiceStarted = false

	private val appData = AppOptions(applicationContext)
	private val userData = UserData(applicationContext)
	private val apiClient = ApiClient(applicationContext)

	private val genders: Array<String> = resources.getStringArray(R.array.genders_array)
	private val ages: Array<String> = resources.getStringArray(R.array.ages_array)
	private val intent = Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
	private val notificationIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (!isUserLoggedIn()) {
			// User is not logged in, do not start the service
			stopSelf()
			return START_NOT_STICKY
		}

		if (!isServiceStarted) {
			// Only start the timer once
			appData.load()
			timer.scheduleAtFixedRate(UpdateTask(), 0L, appData.updateInterval * 60000L)
			registerReceiver(updateReceiver, IntentFilter("com.dmcroww.genderstatus.PREFERENCES_UPDATED"))
			isServiceStarted = true
		}

		return START_STICKY
	}

	private fun isUserLoggedIn(): Boolean {
		return (userData.username.isNotBlank() && userData.password.isNotBlank())
	}

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onDestroy() {
		timer.cancel()
		super.onDestroy()
	}

	inner class UpdateTask: TimerTask() {
		override fun run() {
			fetchDataAndHandleUpdates()
		}
	}

	fun restartTimer() {
		timer.cancel()
		timer = Timer()
		appData.load()
		timer.scheduleAtFixedRate(UpdateTask(), 0L, appData.updateInterval * 60000L)
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun fetchDataAndHandleUpdates() {
		GlobalScope.launch(Dispatchers.Main) {
			val friends = apiClient.fetchFriends()

//
//					val partnerHistory = ApiClient.getData(applicationContext, "history", appData.partner)
//					StorageManager.savePartnerHistory(this@UpdateService, partnerHistory)
//
//				if (StorageManager.getPartner(applicationContext).timestamp != friends.timestamp) {
//					StorageManager.savePartner(this@UpdateService, friends)
//
//					if (friends.timestamp > 0) notify(friends as Array<Person>)
//					sendBroadcast(Intent("com.dmcroww.genderstatus.DATA_UPDATED"))
//				}
		}
	}

	@SuppressLint("MissingPermission")
	private fun notify(friend: Person) {
		val status = friend.status
		val activity = status.activity
		val mood = status.mood
		val gender = genders[status.gender]
		val age = ages[status.age]
		val sus = (status.sus - 1) * 10

		val inboxStyle = NotificationCompat.InboxStyle()
			.addLine("Mood: $mood")
			.addLine("$gender, $age, $sus% sus")

		val notificationBuilder = NotificationCompat.Builder(this, "friends_update_channel")
			.setSmallIcon(R.drawable.android_dark_128)
			.setPriority(2)
			.setContentIntent(notificationIntent)
			.setAutoCancel(true)
			.setContentTitle(activity)
			.setStyle(inboxStyle)


		with(NotificationManagerCompat.from(this)) {
			notify(0, notificationBuilder.build())
		}
	}

	private val updateReceiver = object: BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent?) {
			when (intent?.action) {
				"com.dmcroww.genderstatus.PREFERENCES_UPDATED" -> restartTimer()

				"com.dmcroww.genderstatus.FORCE_UPDATE" -> fetchDataAndHandleUpdates()
			}
		}
	}
}
