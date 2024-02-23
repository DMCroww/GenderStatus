package com.dmcroww.genderstatus.providers

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dmcroww.genderstatus.FriendHistoryActivity
import com.dmcroww.genderstatus.R
import com.dmcroww.genderstatus.entities.AppOptions
import com.dmcroww.genderstatus.entities.Person
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubScreenFragment: Fragment() {
	private lateinit var context: Context
	private lateinit var dataNickname: TextView
	private lateinit var dataActivity: TextView
	private lateinit var dataMood: TextView
	private lateinit var dataAge: TextView
	private lateinit var dataSus: TextView
	private lateinit var dataGender: TextView
	private lateinit var dataDelta: TextView
	private lateinit var dataUpdated: TextView
	private lateinit var buttonHistory: ImageButton

	private lateinit var username: String
	private lateinit var appData: AppOptions

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
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.frag_friend_entry, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val context = requireContext().applicationContext
		appData = AppOptions(context)

		// Initialize UI elements

		dataNickname = view.findViewById(R.id.nickname)
		dataActivity = view.findViewById(R.id.dataActivity)
		dataMood = view.findViewById(R.id.dataMood)
		dataAge = view.findViewById(R.id.dataAge)
		dataSus = view.findViewById(R.id.dataSus)
		dataGender = view.findViewById(R.id.dataGender)
		dataDelta = view.findViewById(R.id.dataDelta)
		dataUpdated = view.findViewById(R.id.dataUpdated)

		buttonHistory = view.findViewById(R.id.button_history)

		buttonHistory.setOnClickListener {
			val intent = Intent(context, FriendHistoryActivity::class.java)
			intent.putExtra("username", username)
			startActivity(intent)
		}
		setTheme(view)

		val genders: Array<String> = resources.getStringArray(R.array.genders_array)
		val ages: Array<String> = resources.getStringArray(R.array.ages_array)

		// Fetch data from SharedStorage based on the username
		val person = Person(context, username)
		person.load()

		dataNickname.text = if (person.nickname.isNotBlank()) person.nickname else person.username
		dataActivity.text = person.status.activity
		dataMood.text = person.status.mood
		dataAge.text = ages[person.status.age]
		dataSus.text = if (person.status.sus > 0) "${(person.status.sus - 1) * 10}%" else "..."
		dataGender.text = genders[person.status.gender]
		dataDelta.text = timestampToDelta(person.status.timestamp)
		dataUpdated.text = timestampToDateTime(person.status.timestamp)

		lifecycleScope.launch {
			val image = StorageManager(context).fetchAvatar(person.status.avatar)
			if (image != null) view.findViewById<ImageView>(R.id.avatar).setImageBitmap(image)
			else view.findViewById<ImageView>(R.id.avatar).setImageDrawable(getDrawable(context, R.drawable.android_128))
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

	/**
	 * Converts a Unix timestamp to a formatted date and time string.
	 */
	private fun timestampToDateTime(unixTimestamp: Long): String {
		return if (unixTimestamp > 0) "(" + SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault()).format(Date(unixTimestamp * 1000L)) + ")"
		else "..."
	}

	/**
	 * Converts a Unix timestamp to a time difference to current time.
	 */
	private fun timestampToDelta(unixTimestamp: Long): String {
		return if (unixTimestamp > 0) DateUtils.getRelativeTimeSpanString(unixTimestamp * 1000L, System.currentTimeMillis(), 60000L).toString()
		else "..."
	}

	private fun setTheme(view: View) {
		// Determine default values based on system theme
		val defaultBackgroundIdx = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 2 else 1

		// Use the default values if the selected values are 0 (indicating "Default")
		val colorsArray = resources.obtainTypedArray(R.array.color_sources)

		val finalColorIdx = if (appData.textColorInt == 0 || appData.background.isBlank()) colorsArray.getColor(defaultBackgroundIdx, 0) else appData.textColorInt

		colorsArray.recycle()

		val bigSize = 32.0f * (appData.fontSize / 100.0f)
		val medSize = 24.0f * (appData.fontSize / 100.0f)
		val smallSize = 20.0f * (appData.fontSize / 100.0f)
		val tinySize = 18.0f * (appData.fontSize / 100.0f)

		val titleActivity = view.findViewById<TextView>(R.id.title_activity)
		val titleMood = view.findViewById<TextView>(R.id.title_mood)
		val titleAge = view.findViewById<TextView>(R.id.title_age)
		val titleSus = view.findViewById<TextView>(R.id.title_sus)
		val titleGender = view.findViewById<TextView>(R.id.title_gender)
		val titleUpdated = view.findViewById<TextView>(R.id.title_updated)

		dataNickname.setTextColor(finalColorIdx)
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
	}
}
