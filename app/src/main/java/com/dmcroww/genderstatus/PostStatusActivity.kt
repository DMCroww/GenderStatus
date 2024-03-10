package com.dmcroww.genderstatus

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.dmcroww.genderstatus.entities.AppOptions
import com.dmcroww.genderstatus.entities.UserData
import com.dmcroww.genderstatus.providers.ApiClient
import com.dmcroww.genderstatus.providers.StorageManager
import kotlinx.coroutines.launch
import org.json.JSONArray

class PostStatusActivity: AppCompatActivity() {
	private lateinit var avatarImage: ImageView
	private lateinit var activityInput: EditText
	private lateinit var moodInput: EditText
	private lateinit var agePreview: TextView
	private lateinit var susPreview: TextView
	private lateinit var genderPreview: TextView
	private lateinit var avatars: JSONArray
	private var avatarsList: MutableList<Bitmap?> = mutableListOf()
	private var avatarsNames: MutableList<String> = mutableListOf()
	private lateinit var userData: UserData
	private lateinit var appData: AppOptions
	private lateinit var storageManager: StorageManager
	private lateinit var genders: Array<String>
	private lateinit var ages: Array<String>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		userData = UserData(applicationContext)
		appData = AppOptions(applicationContext)
		storageManager = StorageManager(applicationContext)
		genders = resources.getStringArray(R.array.genders_array)
		ages = resources.getStringArray(R.array.ages_array)
		AppCompatDelegate.setDefaultNightMode(if (appData.darkMode == 0) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else if (appData.darkMode == 1) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

		setContentView(R.layout.act_update_self)

		// Initialize UI elements
		avatarImage = findViewById(R.id.avatar)
		activityInput = findViewById(R.id.activity_input)
		moodInput = findViewById(R.id.mood_input)
		agePreview = findViewById(R.id.title_age)
		susPreview = findViewById(R.id.title_sus)
		genderPreview = findViewById(R.id.title_gender)



		lifecycleScope.launch {
			val image = storageManager.fetchAvatar(userData.status.avatar)
			if (image != null) {
				avatarImage.setImageBitmap(image)
				findViewById<TextView>(R.id.avatar_hint).text = ""
			} else Toast.makeText(this@PostStatusActivity, "Failed to load image.", Toast.LENGTH_SHORT).show()

			avatars = ApiClient(applicationContext).getArray("get avatars")
			for (i in 0 until avatars.length()) {
				avatarsList.add(storageManager.fetchAvatar(avatars.optString(i)))
				avatarsNames.add(avatars.optString(i))
			}
		}

		avatarImage.setOnClickListener {this.showAvatarDialog()}
		val status = userData.status

		activityInput.setText(status.activity)
		moodInput.setText(status.mood)
		agePreview.text = ages[status.age]
		susPreview.text = if (status.sus > 0) "${(status.sus - 1) * 10}%" else "…"
		genderPreview.text = genders[status.gender]

		setupSliders()

		findViewById<Button>(R.id.button_save).setOnClickListener {
			userData.apply {
				status.activity = activityInput.text.trim().toString()
				status.mood = moodInput.text.trim().toString()
			}
			lifecycleScope.launch {
				ApiClient(applicationContext).postStatus()
				Toast.makeText(applicationContext, "Status updated.", Toast.LENGTH_SHORT).show()
			}
			finish()
		}
	}

	private fun setupSliders() {
		findViewById<SeekBar>(R.id.gender_slider).apply {
			progress = if (userData.status.gender > 0) userData.status.gender else 6
			setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
				override fun onStartTrackingTouch(seekBar: SeekBar?) {}
				override fun onStopTrackingTouch(seekBar: SeekBar?) {}
				override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
					if (fromUser) {
						userData.status.gender = progress
						genderPreview.text = getString(R.string.titleGender, genders[progress])
					}
				}
			})
		}

		findViewById<SeekBar>(R.id.age_slider).apply {
			progress = if (userData.status.age > 0) userData.status.age else 4
			setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
				override fun onStartTrackingTouch(seekBar: SeekBar?) {}
				override fun onStopTrackingTouch(seekBar: SeekBar?) {}
				override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
					if (fromUser) {
						userData.status.age = progress
						agePreview.text = getString(R.string.titleAge, ages[progress])
					}
				}
			})
		}

		findViewById<SeekBar>(R.id.sus_slider).apply {
			progress = if (userData.status.sus > 0) userData.status.sus else 6
			setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
				override fun onStartTrackingTouch(seekBar: SeekBar?) {}
				override fun onStopTrackingTouch(seekBar: SeekBar?) {}
				override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
					if (fromUser) {
						userData.status.sus = progress
						susPreview.text = getString(R.string.titleSus, ((progress - 1) * 10).toString())
					}
				}
			})
		}
	}

	private fun showAvatarDialog() {
		val dialogView = layoutInflater.inflate(R.layout.dialog_image_picker, null)
		val gridView = dialogView.findViewById<GridView>(R.id.imageGridView)

		val adapter = ImageGridAdapter(this, avatarsList)
		gridView.adapter = adapter

		val dialog = AlertDialog.Builder(this).setTitle("Select Avatar Image").setView(dialogView).setNegativeButton("Cancel") {dialog, _ ->
			dialog.dismiss()
		}.create()

		gridView.setOnItemClickListener {_, _, position, _ ->
			userData.status.avatar = avatarsNames[position]
			avatarImage.setImageBitmap(avatarsList[position])
			dialog.dismiss()
		}

		dialog.show()
	}

	class ImageGridAdapter(private val context: Context, private val avatarList: List<Bitmap?>): BaseAdapter() {

		override fun getCount(): Int {
			return avatarList.size
		}

		override fun getItem(position: Int): Any? {
			return avatarList[position]
		}

		override fun getItemId(position: Int): Long {
			return position.toLong()
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
			val imageView = if (convertView == null) ImageView(context).apply {
				layoutParams = AbsListView.LayoutParams(300, 300) // Adjust size as needed
				scaleType = ImageView.ScaleType.CENTER_CROP
				setPadding(8, 8, 8, 8) // Adjust padding as needed
			}
			else convertView as ImageView

			val bitmap = avatarList[position]
			if (bitmap != null) imageView.setImageBitmap(bitmap)

			return imageView
		}
	}
}