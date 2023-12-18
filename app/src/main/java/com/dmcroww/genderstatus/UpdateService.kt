package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
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

	private val notificationChannelId = "gender_status_channel"
	private var interval = 300000L
	private var timer = Timer()

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		interval = AppOptions.getData(applicationContext).updateInterval * 60000L
		timer.scheduleAtFixedRate(UpdateTask(), 0L, interval)
		registerReceiver(updateReceiver, IntentFilter("com.dmcroww.genderstatus.PREFERENCES_UPDATED"))
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
		interval = AppOptions.getData(applicationContext).updateInterval * 60000L
		timer = Timer()
		timer.scheduleAtFixedRate(UpdateTask(), 100L, interval)
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun fetchDataAndHandleUpdates() {
		val appData = AppOptions.getData(this@UpdateService)
		if (appData.username != "" && appData.partner != "") {
			GlobalScope.launch(Dispatchers.Main) {
				val partner = ApiClient.fetchPartner(applicationContext)

					val partnerHistory = ApiClient.getData(applicationContext, "history", appData.partner)
					StorageManager.savePartnerHistory(this@UpdateService, partnerHistory)

				if (StorageManager.getPartner(applicationContext).timestamp != partner.timestamp) {
					StorageManager.savePartner(this@UpdateService, partner)

					if (partner.timestamp > 0) notify(partner)
					sendBroadcast(Intent("com.dmcroww.genderstatus.DATA_UPDATED"))
				}
			}
		} else {
			sendBroadcast(Intent("com.dmcroww.genderstatus.DATA_FAILED"))
		}
	}

	@SuppressLint("MissingPermission")
	private fun notify(partner: Person) {
		val genders: Array<String> = resources.getStringArray(R.array.genders_array)
		val ages: Array<String> = resources.getStringArray(R.array.ages_array)
		val intent = Intent(this, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		}
		val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
		val name = "Partner Status Updates"
		val importance = NotificationManager.IMPORTANCE_LOW
		val id = notificationChannelId
		val channel = NotificationChannel(id, name, importance)
		val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannel(channel)

		val activity = partner.activity
		val mood = partner.mood
		val gender = genders[partner.gender]
		val age = ages[partner.age]
		val sus = (partner.sus - 1) * 10

		val inboxStyle = NotificationCompat.InboxStyle()
			.addLine("Mood: $mood")
			.addLine("$gender, $age, $sus% sus")

		val notificationBuilder = NotificationCompat.Builder(this, id)
			.setSmallIcon(R.drawable.android_dark_128)
			.setPriority(importance)
			.setContentIntent(pendingIntent)
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
