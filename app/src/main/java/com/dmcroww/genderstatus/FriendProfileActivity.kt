package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.dmcroww.genderstatus.entities.AppOptions
import com.dmcroww.genderstatus.entities.Person
import com.dmcroww.genderstatus.providers.ApiClient
import com.dmcroww.genderstatus.providers.StorageManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")

class FriendProfileActivity: AppCompatActivity() {
	private lateinit var context: Context
	private lateinit var storageManager: StorageManager
	private lateinit var appData: AppOptions
	private lateinit var apiClient: ApiClient
	private lateinit var historyDataLayout: LinearLayout

	private lateinit var genders: Array<String>
	private lateinit var ages: Array<String>

	private var bigSize = 1f
	private var medSize = 1f
	private var smallSize = 1f
	private var tinySize = 1f

	private lateinit var username: String

	private lateinit var person: Person

	private val rotateAnimation = RotateAnimation(
		0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
	).apply {
		duration = 2000 // Set the duration of the rotation
		repeatCount = -1 // Make the rotation continuous
	}

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		context = this
		storageManager = StorageManager(context)
		appData = AppOptions(context)
		apiClient = ApiClient(context)
		genders = resources.getStringArray(R.array.genders_array)
		ages = resources.getStringArray(R.array.ages_array)

		bigSize = 20.0f * appData.fontSize
		medSize = 20.0f * appData.fontSize
		smallSize = 18.0f * appData.fontSize
		tinySize = 16.0f * appData.fontSize

		username = intent.getStringExtra("username")!!
		person = Person(context, username)
		person.load()

		setContentView(R.layout.act_friend_profile)

		initializeUI()
		populateData()
		loading()
		refreshHistory()
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun initializeUI() {

		historyDataLayout = findViewById(R.id.history_data)
		findViewById<ImageView>(R.id.button_history).apply {
			setColorFilter(appData.finalColorIdx)
			setOnClickListener {
//			loadMoreHistory()
			}
		}

		findViewById<ImageButton>(R.id.button_editDisplayName).apply {
			setColorFilter(appData.finalColorIdx)
			setOnClickListener {
				showTextInputDialog("Change Displayed Name", "Input new Display name for user @$username") {
					person.displayName = it
					person.save()
					findViewById<TextView>(R.id.nickname).text = it
				}
			}
		}

		val bckArr = resources.obtainTypedArray(R.array.background_sources)
		// Determine default values based on system theme
		val defaultBackgroundIdx = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) 2 else 1

		// Use the default values if the selected values are 0 (indicating "Default")
		findViewById<ConstraintLayout>(R.id.main).setBackgroundResource(bckArr.getResourceId(defaultBackgroundIdx, 0))

		GlobalScope.launch(Dispatchers.IO) {
			val backgroundImage = if (appData.background != "") storageManager.fetchBackground(appData.background) else null
			runOnUiThread {
				findViewById<ImageView>(R.id.backgroundImage).setImageBitmap(backgroundImage)
			}
		}


		bckArr.recycle()
	}

	private fun populateData() {
		val status = person.status

		findViewById<TextView>(R.id.nickname).apply {
			setTextColor(appData.finalColorIdx)
			textSize = bigSize
			text = if (person.displayName.isNotBlank()) person.displayName else if (person.nickname.isNotBlank()) person.nickname else username
		}

		findViewById<TextView>(R.id.title_activity).apply {
			setTextColor(appData.finalColorIdx)
			textSize = bigSize
		}
		findViewById<TextView>(R.id.dataActivity).apply {
			setTextColor(appData.finalColorIdx)
			textSize = medSize
			text = status.activity
		}
		findViewById<TextView>(R.id.title_mood).apply {
			setTextColor(appData.finalColorIdx)
			textSize = bigSize
		}
		findViewById<TextView>(R.id.dataMood).apply {
			setTextColor(appData.finalColorIdx)
			textSize = medSize
			text = status.mood
		}
		findViewById<TextView>(R.id.title_sus).apply {
			setTextColor(appData.finalColorIdx)
			textSize = smallSize
			text = getString(R.string.titleSus, if (status.sus > 0) "${(status.sus - 1) * 10}%" else "...")
		}
		findViewById<TextView>(R.id.title_age).apply {
			setTextColor(appData.finalColorIdx)
			textSize = smallSize
			text = getString(R.string.titleSus, ages[status.age])
		}
		findViewById<TextView>(R.id.title_gender).apply {
			setTextColor(appData.finalColorIdx)
			textSize = smallSize
			text = getString(R.string.titleGender, genders[status.gender])
		}
		findViewById<TextView>(R.id.title_updated).apply {
			setTextColor(appData.finalColorIdx)
			textSize = tinySize
			text = getString(R.string.titleUpdated, timestampToRelativePlus(status.timestamp))
		}
		findViewById<TextView>(R.id.title_history).apply {
			setTextColor(appData.finalColorIdx)
			textSize = bigSize
		}


		lifecycleScope.launch {
			val image = StorageManager(context).fetchAvatar(status.avatar)
			findViewById<ImageView>(R.id.avatar).apply {
				backgroundTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(appData.finalColorIdx))
				if (image != null) {
					setImageBitmap(image)
					setBackgroundColor(0)
				}
			}
		}
	}

	private fun refreshHistory() {
		historyDataLayout.removeAllViews()

		lifecycleScope.launch {
			try {
				val history = apiClient.fetchFriendHistory(username)

				// Check if the stored string is not null
				if (history.isEmpty()) {
					Toast.makeText(context, "Friend history empty!", Toast.LENGTH_SHORT).show()
					loaded()
				} else {
					for (i in history.indices) {
						val status = history.get(i)

						val entryView = LayoutInflater.from(context).inflate(R.layout.frag_friend_history_entry, historyDataLayout, false)

						val image = storageManager.fetchAvatar(status.avatar)

						entryView.findViewById<ImageView>(R.id.avatar).apply {
							backgroundTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(appData.finalColorIdx))
							if (image != null) {
								setImageBitmap(image)
								setBackgroundColor(0)
							}
						}

						entryView.findViewById<TextView>(R.id.activity).apply {
							text = status.activity
							setTextColor(appData.finalColorIdx)
							textSize = bigSize
						}

						entryView.findViewById<TextView>(R.id.mood).apply {
							text = status.mood
							setTextColor(appData.finalColorIdx)
							textSize = medSize
						}
						entryView.findViewById<TextView>(R.id.date).apply {
							text = timestampToRelativePlus(status.timestamp)
							setTextColor(appData.finalColorIdx)
							textSize = tinySize
						}
						entryView.findViewById<TextView>(R.id.other).apply {
							text = "${genders[status.gender]} ${ages[status.age]}, ${if (status.sus > 0) "${(status.sus - 1) * 10}%sus" else "..."}"
							setTextColor(appData.finalColorIdx)
							textSize = smallSize
						}
						runOnUiThread {
							historyDataLayout.addView(entryView)
						}
					}
					// Add empty Space with set height (16dp in this example)
					val space = Space(context)
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
			historyDataLayout.startAnimation(AlphaAnimation(historyDataLayout.alpha, 1F).apply {duration = 250; fillAfter = true})
			findViewById<ImageView>(R.id.loadingImage).apply {clearAnimation(); visibility = View.GONE}
		}
	}

	private fun loading(animateOut: Boolean = false) {
		runOnUiThread {
			if (animateOut)
				historyDataLayout.startAnimation(AlphaAnimation(1F, 0.25F).apply {duration = 250; fillAfter = true})
			findViewById<ImageView>(R.id.loadingImage).apply {startAnimation(rotateAnimation); visibility = View.VISIBLE}
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
