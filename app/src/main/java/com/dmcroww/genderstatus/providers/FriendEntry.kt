package com.dmcroww.genderstatus.providers

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
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
		person.load()

		populateData(view)
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

	private fun populateData(view: View) {
		val status = person.status

		view.findViewById<TextView>(R.id.nickname).apply {
			setTextColor(appData.finalColorIdx)
			textSize = bigSize
			text = if (person.displayName.isNotBlank()) person.displayName else if (person.nickname.isNotBlank()) person.nickname else username
			setOnClickListener {
				startActivity(Intent(context, FriendProfileActivity::class.java).putExtra("username", username))
			}
//			setOnClickListener {
//				showTextInputDialog("Change Displayed Name", "Input new Display name for user @$username") {
//					person.displayName = it
//					person.save()
//					context.sendBroadcast(Intent("com.dmcroww.genderstatus.DATA_UPDATED"))
//				}
//			}
		}

		view.findViewById<TextView>(R.id.title_activity).apply {
			setTextColor(appData.finalColorIdx)
			textSize = bigSize
		}
		view.findViewById<TextView>(R.id.dataActivity).apply {
			setTextColor(appData.finalColorIdx)
			textSize = medSize
			text = status.activity
		}
		view.findViewById<TextView>(R.id.title_mood).apply {
			setTextColor(appData.finalColorIdx)
			textSize = bigSize
		}
		view.findViewById<TextView>(R.id.dataMood).apply {
			setTextColor(appData.finalColorIdx)
			textSize = medSize
			text = status.mood
		}
		view.findViewById<TextView>(R.id.dataUpdated).apply {
			setTextColor(appData.finalColorIdx)
			textSize = tinySize
			text = getString(R.string.titleUpdated, timestampToRelativePlus(status.timestamp))
		}


		lifecycleScope.launch {
			val image = StorageManager(context).fetchAvatar(status.avatar)
			view.findViewById<ImageView>(R.id.avatar).apply {
				backgroundTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(appData.finalColorIdx))
				if (image != null) {
					setImageBitmap(image)
					setBackgroundColor(0)
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

	fun showTextInputDialog(title: String, message: String, isPassword: Boolean = false, callback: (String) -> Unit) {
		val dialogView = layoutInflater.inflate(R.layout.dialog_text_input, null)
		val editText = dialogView.findViewById<EditText>(R.id.inputText)
		editText.setText("")
		if (isPassword) editText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD

		val dialog = AlertDialog.Builder(context)
			.setTitle(title)
			.setMessage(message)
			.setView(dialogView)
			.setNegativeButton("Cancel") {dialog, _ ->
				dialog.dismiss()
			}
			.setPositiveButton("Save") {dialog, _ ->
				callback(editText.text.toString())
				dialog.dismiss()
			}
			.create()
		dialog.show()
	}
}
