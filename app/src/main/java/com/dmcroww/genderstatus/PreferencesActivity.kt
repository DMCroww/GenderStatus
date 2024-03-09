@file:OptIn(DelicateCoroutinesApi::class)

package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dmcroww.genderstatus.entities.AppOptions
import com.dmcroww.genderstatus.entities.UserData
import com.dmcroww.genderstatus.providers.ApiClient
import com.dmcroww.genderstatus.providers.StorageManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PreferencesActivity: AppCompatActivity() {
	private lateinit var appData: AppOptions
	private lateinit var userData: UserData
	private lateinit var intervalView: TextView
	private lateinit var updateIntBar: SeekBar
	private lateinit var fontSizeView: TextView
	private lateinit var fontSizeBar: SeekBar
	private lateinit var backgrounds: JSONArray
	private var backgroundsList: MutableList<Bitmap?> = mutableListOf()
	private var backgroundsNames: MutableList<String> = mutableListOf()
	private var backgroundsLoaded: Boolean = false
	private lateinit var storageManager: StorageManager
	private lateinit var apiClient: ApiClient

	@SuppressLint("SetTextI18n")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.act_preferences)
		// Initialize SharedPreferences
		appData = AppOptions(applicationContext)
		userData = UserData(applicationContext)
		storageManager = StorageManager(applicationContext)
		apiClient = ApiClient(applicationContext)

		// Initialize UI elements
		updateIntBar = findViewById(R.id.updateInt_bar)
		intervalView = findViewById(R.id.updateInt_data)
		fontSizeBar = findViewById(R.id.fontsize_bar)
		fontSizeView = findViewById(R.id.fontsize_data)

		// Load saved preferences
		updateIntBar.progress = appData.updateInterval
		intervalView.text = updateIntBar.progress.toString()
		fontSizeBar.progress = (appData.fontSize * 20f).toInt()
		fontSizeView.text = (fontSizeBar.progress * 100).toString() + "%"



		updateIntBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				intervalView.text = progress.toString()
				appData.updateInterval = progress
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})
		fontSizeBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				val value = progress * 5
				fontSizeView.text = "$value%"
				appData.fontSize = value / 100.0f
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		lifecycleScope.launch {
			backgrounds = apiClient.getArray("get backgrounds")

			for (i in 0 until backgrounds.length()) {
				val filename = backgrounds.optString(i)
				val img = storageManager.fetchBackground(filename)
				backgroundsList.add(img)
				backgroundsNames.add(filename)
			}
			backgroundsLoaded = true
		}

		// Save button click listener
		val saveButton = findViewById<Button>(R.id.save_button)
		saveButton.setOnClickListener {
			try {
				appData.save()
				Toast.makeText(applicationContext, "Preferences saved", Toast.LENGTH_SHORT).show()
				finish()
				sendBroadcast(Intent("com.dmcroww.genderstatus.PREFERENCES_UPDATED"))
			} catch (e: Exception) {
				e.printStackTrace()
				Toast.makeText(applicationContext, "Error saving preferences", Toast.LENGTH_SHORT).show()
			}

		}

		// Background selection click listener
		findViewById<Button>(R.id.button_background).setOnClickListener {
			val options = arrayOf("Predefined Backgrounds", "Select from Device")

			val builder = AlertDialog.Builder(this)
			builder.setTitle("Select Background Source")
			builder.setItems(options) {_, which ->
				when (which) {
					0 -> showPredefinedBackgroundDialog()
					1 -> openImagePicker()
				}
			}
			builder.show()
		}
		// Color selection click listener
		findViewById<Button>(R.id.button_color).setOnClickListener {
			val colorPickerDialog = ColorPickerDialog(this, appData.textColorInt) {selectedColor ->
				appData.textColorInt = selectedColor
			}
			colorPickerDialog.show()
		}
	}

	private val handler = Handler(Looper.getMainLooper())

	private fun showPredefinedBackgroundDialog() {
		val dialogView = layoutInflater.inflate(R.layout.dialog_image_picker, null)
		val gridView = dialogView.findViewById<GridView>(R.id.imageGridView)

		// Wait until backgrounds are loaded
		waitUntilBackgroundsLoaded {
			val adapter = BackgroundGridAdapter(this, backgroundsList)
			gridView.adapter = adapter

			val dialog = AlertDialog.Builder(this)
				.setTitle("Select Predefined Background")
				.setView(dialogView)
				.setNegativeButton("Cancel") {dialog, _ ->
					dialog.dismiss()
				}
				.create()

			gridView.setOnItemClickListener {_, _, position, _ ->
				appData.background = backgroundsNames[position]
				dialog.dismiss()
			}

			dialog.show()
		}
	}

	private fun waitUntilBackgroundsLoaded(callback: () -> Unit) {
		if (!backgroundsLoaded) {
			handler.postDelayed({
				waitUntilBackgroundsLoaded(callback)
			}, 1000) // Wait for 1 second
		} else {
			callback.invoke()
		}
	}

	private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
		if (result.resultCode == RESULT_OK) {
			val intent: Intent? = result.data
			if (intent != null && intent.data != null) {
				val selectedImageUri: Uri = intent.data!!

				// Use coroutine to perform I/O operation asynchronously
				GlobalScope.launch(Dispatchers.IO) {
					// Read image data and save it to cache
					val inputStream: InputStream? = contentResolver.openInputStream(selectedImageUri)
					if (inputStream != null) {
						try {
							val cacheDir = cacheDir
							val cacheFile = File(cacheDir, "cachedUserBackground.jpg")
							val outputStream = FileOutputStream(cacheFile)

							inputStream.use {input ->
								outputStream.use {output ->
									input.copyTo(output, bufferSize = 4 * 1024)
								}
							}
							outputStream.close()
							inputStream.close()
							appData.background = cacheFile.absolutePath
						} catch (e: Exception) {
							Log.e("background", "Error saving image to cache: ${e.message}")
						}
					}
				}
			}
		}
	}

	private fun openImagePicker() {
		val intent = Intent(Intent.ACTION_GET_CONTENT)
		intent.type = "image/*"
		imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"))
	}

	class BackgroundGridAdapter(private val context: Context, private val backgroundList: List<Bitmap?>): BaseAdapter() {

		override fun getCount(): Int {
			return backgroundList.size
		}

		override fun getItem(position: Int): Any? {
			return backgroundList[position]
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

			val bitmap = backgroundList[position]
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap)
			}

			return imageView
		}
	}

	class ColorPickerDialog(context: Context, private val currentColor: Int, private val onColorSelected: (Int) -> Unit) {

		@SuppressLint("InflateParams")
		private val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
		private val hueSeekBar = dialogView.findViewById<SeekBar>(R.id.hueSeekBar)
		private val saturationSeekBar = dialogView.findViewById<SeekBar>(R.id.saturationSeekBar)
		private val valueSeekBar = dialogView.findViewById<SeekBar>(R.id.valueSeekBar)
		private val previewText = dialogView.findViewById<TextView>(R.id.colorValueText)

		init {
			setupSeekBars()
			setColor(currentColor)
		}

		private fun setupSeekBars() {

			val hsv = FloatArray(3)
			Color.colorToHSV(currentColor, hsv)
			hueSeekBar.progress = (hsv[0] * 10).toInt()
			saturationSeekBar.progress = (hsv[1] * 1000).toInt()
			valueSeekBar.progress = (hsv[2] * 1000).toInt()

			hueSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener())
			saturationSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener())
			valueSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener())
		}

		private fun createSeekBarChangeListener(): SeekBar.OnSeekBarChangeListener {
			return object: SeekBar.OnSeekBarChangeListener {
				override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
					val hue = hueSeekBar.progress.toFloat() / 10.0f
					val saturation = saturationSeekBar.progress.toFloat() / 1000.0f
					val value = valueSeekBar.progress.toFloat() / 1000.0f

					setColor(Color.HSVToColor(floatArrayOf(hue, saturation, value)))
				}

				override fun onStartTrackingTouch(seekBar: SeekBar?) {}

				override fun onStopTrackingTouch(seekBar: SeekBar?) {}
			}
		}

		private fun setColor(color: Int) {
			val hsv = FloatArray(3)
			Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv)
			previewText.setTextColor(color)
		}

		fun show() {
			val builder = AlertDialog.Builder(dialogView.context)
			builder.setView(dialogView)
			builder.setPositiveButton("OK") {_, _ ->
				onColorSelected(
					Color.HSVToColor(
						floatArrayOf(
							hueSeekBar.progress.toFloat() / 10.0f, saturationSeekBar.progress.toFloat() / 1000.0f, valueSeekBar.progress.toFloat() / 1000.0f
						)
					)
				)
			}
			builder.setNegativeButton("Cancel") {dialog, _ ->
				dialog.dismiss()
			}

			val dialog = builder.create()
			dialog.show()
		}
	}
}
