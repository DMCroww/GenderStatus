package com.dmcroww.genderstatus.providers

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
import com.dmcroww.genderstatus.MainActivity
import com.dmcroww.genderstatus.R
import com.dmcroww.genderstatus.entities.AppOptions
import com.dmcroww.genderstatus.entities.Person
import com.dmcroww.genderstatus.entities.Status
import com.dmcroww.genderstatus.entities.UserData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask

class UpdateService: Service() {

	private var timer = Timer()
	private var isServiceStarted = false

	private lateinit var appData: AppOptions
	private lateinit var apiClient: ApiClient
	private lateinit var userData: UserData

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

		appData = AppOptions(applicationContext)
		apiClient = ApiClient(applicationContext)
		userData = UserData(applicationContext)


		if (userData.username.isBlank() || userData.password.isBlank()) {
			// User is not logged in, do not start the service
			stopSelf()
			return START_NOT_STICKY
		}

		if (!isServiceStarted) {
			// Only start the timer once
			appData.load()
			timer.scheduleAtFixedRate(UpdateTask(), 0L, appData.updateInterval * 60000L)

			// Register broadcast receiver for data updates
			val filter = IntentFilter().apply {
				addAction("com.dmcroww.genderstatus.PREFERENCES_UPDATED")
				addAction("com.dmcroww.genderstatus.FORCE_UPDATE")
			}
			registerReceiver(updateReceiver, filter)

			isServiceStarted = true
		}

		return START_STICKY
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
		appData = AppOptions(applicationContext)
		appData.load()
		timer.scheduleAtFixedRate(UpdateTask(), 0L, appData.updateInterval * 60000L)
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun fetchDataAndHandleUpdates() {
		GlobalScope.launch(Dispatchers.Main) {
			val friends = apiClient.fetchFriends()

			// Update friends list concisely
			userData.friends = friends.keys.toTypedArray()

			// Iterate through friends, updating and notifying when necessary
			friends.forEach {(key, obj) ->
				Person(applicationContext, key).apply {
					load()
					val newStatus = Status(obj.optJSONObject("status") ?: JSONObject())
					val newNick = obj.getString("nickname")

					if (nickname != newNick) {
						nickname = newNick
						save()
					}

					if (status.timestamp < newStatus.timestamp) {
						status = newStatus
						save()
						notify(this)
					}
				}
			}
			userData.save()
			sendBroadcast(Intent("com.dmcroww.genderstatus.DATA_UPDATED"))
		}
	}

	//
	@SuppressLint("MissingPermission")
	private fun notify(friend: Person) {
		val genders: Array<String> = resources.getStringArray(R.array.genders_array)
		val ages: Array<String> = resources.getStringArray(R.array.ages_array)

		val status = friend.status
		val activity = status.activity
		val mood = status.mood
		val gender = genders[status.gender]
		val age = ages[status.age]
		val sus = if (status.sus > 0) "${(status.sus - 1) * 10}% sus" else "..."

		val inboxStyle = NotificationCompat.InboxStyle()
			.addLine("Mood: $mood")
			.addLine("$gender, $age, $sus")

		val intent = Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		val notificationIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

		val notificationBuilder = NotificationCompat.Builder(this, "friends_update_channel")
			.setSmallIcon(R.drawable.android_128)
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
