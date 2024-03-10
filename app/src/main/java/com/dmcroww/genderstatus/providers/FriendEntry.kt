package com.dmcroww.genderstatus.providers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dmcroww.genderstatus.FriendProfileActivity
import com.dmcroww.genderstatus.R
import com.dmcroww.genderstatus.entities.AppOptions
import com.dmcroww.genderstatus.entities.Person
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubScreenFragment: Fragment() {
	private lateinit var context: Context
	private lateinit var view: View
	private lateinit var storageManager: StorageManager
	private lateinit var appData: AppOptions
	private lateinit var apiClient: ApiClient

	private lateinit var genders: Array<String>
	private lateinit var ages: Array<String>

	private var bigSize = 1f
	private var medSize = 1f
	private var smallSize = 1f
	private var tinySize = 1f

	private lateinit var username: String

	private lateinit var person: Person

	private var isReceiverRegistered = false
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments?.let {
			username = it.getString("username", "")
		}
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		this.context = context
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.frag_friend_entry, container, false)
	}

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
//				val context = requireContext().applicationContext
		this.view = view
		storageManager = StorageManager(context)
		appData = AppOptions(context)

		apiClient = ApiClient(context)
		genders = resources.getStringArray(R.array.genders_array)
		ages = resources.getStringArray(R.array.ages_array)

		bigSize = 20.0f * appData.fontSize
		medSize = 20.0f * appData.fontSize
		smallSize = 18.0f * appData.fontSize
		tinySize = 16.0f * appData.fontSize

		person = Person(context, username)

		// Register broadcast receiver for data updates
		val filter = IntentFilter()
		filter.addAction("com.dmcroww.genderstatus.DATA_UPDATED")
		filter.addAction("com.dmcroww.genderstatus.PREFERENCES_UPDATED")


		if (!isReceiverRegistered) {
			context.registerReceiver(updateReceiver, filter)
			isReceiverRegistered = true
		}
		populateData(context)
	}

	override fun onStop() {
		super.onStop()
		if (isReceiverRegistered) {
			context.unregisterReceiver(updateReceiver)
			isReceiverRegistered = false
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		if (isReceiverRegistered) {
			context.unregisterReceiver(updateReceiver)
			isReceiverRegistered = false
		}
	}

	override fun onDetach() {
		super.onDetach()
		if (isReceiverRegistered) {
			context.unregisterReceiver(updateReceiver)
			isReceiverRegistered = false
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()

		// Unregister the receiver only if it's registered
		if (isReceiverRegistered) {
			context.unregisterReceiver(updateReceiver)
			isReceiverRegistered = false
		}
	}

	companion object {
		fun newInstance(username: String): SubScreenFragment {
			val fragment = SubScreenFragment()
			val args = Bundle()
			args.putString("username", username)
			fragment.arguments = args
			return fragment
		}
	}

	private fun populateData(context: Context) {
		val status = person.status

		view.findViewById<TextView>(R.id.nickname).apply {
			textSize = bigSize
			text = if (person.displayName.isNotBlank()) person.displayName else if (person.nickname.isNotBlank()) person.nickname else username
			setOnClickListener {
				startActivity(Intent(context, FriendProfileActivity::class.java).putExtra("username", username))
			}
		}

		view.findViewById<TextView>(R.id.title_activity).textSize = bigSize
		view.findViewById<TextView>(R.id.title_mood).textSize = bigSize

		view.findViewById<TextView>(R.id.dataActivity).apply {
			textSize = medSize
			text = status.activity
		}

		view.findViewById<TextView>(R.id.dataMood).apply {
			textSize = medSize
			text = status.mood
		}
		view.findViewById<TextView>(R.id.dataUpdated).apply {
			textSize = tinySize
			text = getString(R.string.titleUpdated, timestampToRelativePlus(status.timestamp))
		}


		lifecycleScope.launch {
			val image = StorageManager(context).fetchAvatar(status.avatar)
			view.findViewById<ImageView>(R.id.avatar).apply {
//				backgroundTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(appData.finalColorIdx))
				if (image != null) {
					setImageBitmap(image)
					setBackgroundColor(0)
				}
				setOnClickListener {
					startActivity(Intent(context, FriendProfileActivity::class.java).putExtra("username", username))
				}
			}
		}
	}

	private fun timestampToRelativePlus(unixTimestamp: Long): String {
		val timestamp = unixTimestamp * 1000L
		val relative = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

		val dateFormat = SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
		return "$relative ($dateFormat)"
	}

	/**
	 * Sets the app's theme based on user preferences.
	 */
	private fun setTheme() {
		lifecycleScope.launch {
			appData.setBackground(view.findViewById(R.id.main_window), view.findViewById(R.id.backgroundImage))

			val nightMode = when (appData.darkMode) {
				1 -> AppCompatDelegate.MODE_NIGHT_YES
				2 -> AppCompatDelegate.MODE_NIGHT_NO
				else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
			}
			AppCompatDelegate.setDefaultNightMode(nightMode)
			when (appData.theme) {
				1 -> context.setTheme(R.style.AppTheme_Blue)
				2 -> context.setTheme(R.style.AppTheme_Pink)
				3 -> context.setTheme(R.style.AppTheme_Purple)
				4 -> context.setTheme(R.style.AppTheme_Magenta)
				5 -> context.setTheme(R.style.AppTheme_Red)
				6 -> context.setTheme(R.style.AppTheme_Orange)
				7 -> context.setTheme(R.style.AppTheme_Yellow)
				8 -> context.setTheme(R.style.AppTheme_Green)
				else -> context.setTheme(R.style.AppTheme) // Default theme
			}
		}
	}

	/**
	 * Broadcast receiver to handle updates and failures in data retrieval.
	 */
	private val updateReceiver = object: BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent?) {
			when (intent?.action) {
				"com.dmcroww.genderstatus.DATA_UPDATED" -> populateData(context)
				"com.dmcroww.genderstatus.PREFERENCES_UPDATED" -> setTheme()
			}
		}
	}
}