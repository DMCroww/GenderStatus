package com.dmcroww.genderstatus

import android.annotation.SuppressLint
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray

class UpdateSelf: AppCompatActivity() {
	private lateinit var avatarImage: ImageView
	private lateinit var activityInput: EditText
	private lateinit var moodInput: EditText
	private lateinit var ageSlider: SeekBar
	private lateinit var agePreview: TextView
	private lateinit var susSlider: SeekBar
	private lateinit var susPreview: TextView
	private lateinit var genderSlider: SeekBar
	private lateinit var genderPreview: TextView
	private lateinit var avatars: JSONArray
	private lateinit var avatarsList: List<Bitmap?>
	private lateinit var avatarsNames: List<String>
	private var userData = UserData(applicationContext)
	private val storageManager = StorageManager(applicationContext)
	private val genders: Array<String> = resources.getStringArray(R.array.genders_array)
	private val ages: Array<String> = resources.getStringArray(R.array.ages_array)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.update_self)

		// Initialize UI elements
		avatarImage = findViewById(R.id.avatar)
		activityInput = findViewById(R.id.activity_input)
		moodInput = findViewById(R.id.mood_input)
		ageSlider = findViewById(R.id.age_slider)
		agePreview = findViewById(R.id.age_preview)
		susSlider = findViewById(R.id.sus_slider)
		susPreview = findViewById(R.id.sus_preview)
		genderSlider = findViewById(R.id.gender_slider)
		genderPreview = findViewById(R.id.gender_preview)

		lifecycleScope.launch {
			val image = storageManager.fetchImage("avatars", userData.status.avatar)
			if (image != null) {
				avatarImage.setImageBitmap(image)
				findViewById<TextView>(R.id.avatar_hint).text = ""
			} else {
				Toast.makeText(this@UpdateSelf, "Failed to load image from cache", Toast.LENGTH_SHORT).show()
			}

			avatars = ApiClient(applicationContext).getArray("fetch avatars")
			avatarsList = listOf(avatars.let {
				storageManager.fetchImage("avatars", it.toString())
			})
			avatarsNames = listOf(avatars.toString())
		}

		avatarImage.setOnClickListener {
			this.showAvatarDialog()
		}

		activityInput.setText(userData.status.activity)
		moodInput.setText(userData.status.mood)
		ageSlider.progress = if (userData.status.age > 0) userData.status.age else 4
		agePreview.text = resources.getStringArray(R.array.ages_array)[userData.status.age]
		susSlider.progress = if (userData.status.sus > 0) userData.status.sus else 6
		susPreview.text = if (userData.status.sus > 0) "${(userData.status.sus - 1) * 10}%" else "…"
		genderSlider.progress = if (userData.status.gender > 0) userData.status.gender else 6
		genderPreview.text = resources.getStringArray(R.array.genders_array)[userData.status.gender]


		setupSeekBarListeners()

		val saveButton = findViewById<Button>(R.id.button_save)
		saveButton.setOnClickListener {
			userData.status.activity = activityInput.text.toString().trim()
			userData.status.mood = moodInput.text.toString().trim()
			userData.save()
			lifecycleScope.launch {
				ApiClient(applicationContext).postStatus()
				Toast.makeText(applicationContext, "Status updated.", Toast.LENGTH_SHORT).show()
			}
			finish()
		}
	}

	private fun setupSeekBarListeners() {

		genderSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			@SuppressLint("SetTextI18n")
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					userData.status.gender = progress
					genderPreview.text = genders[progress]
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		ageSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			@SuppressLint("SetTextI18n")
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					userData.status.age = progress
					agePreview.text = ages[progress]
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		susSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			@SuppressLint("SetTextI18n")
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					userData.status.sus = progress
					susPreview.text = ((progress - 1) * 10).toString() + "%"
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})
	}

	private fun showAvatarDialog() {
		val dialogView = layoutInflater.inflate(R.layout.dialog_image_picker, null)
		val gridView = dialogView.findViewById<GridView>(R.id.imageGridView)

		val adapter = ImageGridAdapter(this, avatarsList)
		gridView.adapter = adapter

		val dialog = AlertDialog.Builder(this)
			.setTitle("Select Avatar Image")
			.setView(dialogView)
			.setNegativeButton("Cancel") {dialog, _ ->
				dialog.dismiss()
			}
			.create()

		gridView.setOnItemClickListener {_, _, position, _ ->
			userData.status.avatar = avatarsNames[position]
			avatarImage.setImageBitmap(avatarsList[position])
			dialog.dismiss()
		}

		dialog.show()
	}

	class ImageGridAdapter(private val context: Context, private val avatarList: List<Bitmap?>):
	 BaseAdapter() {

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
			val imageView: ImageView
			if (convertView == null) {
				imageView = ImageView(context)
				imageView.layoutParams = AbsListView.LayoutParams(300, 300) // Adjust size as needed
				imageView.scaleType = ImageView.ScaleType.CENTER_CROP
				imageView.setPadding(8, 8, 8, 8) // Adjust padding as needed
			} else {
				imageView = convertView as ImageView
			}

			val bitmap = avatarList[position]
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap)
			}

			return imageView
		}
	}
}
