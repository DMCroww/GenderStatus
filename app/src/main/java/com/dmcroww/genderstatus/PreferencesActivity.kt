@file:OptIn(DelicateCoroutinesApi::class)

package com.dmcroww.genderstatus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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

		val fontSizeView = findViewById<TextView>(R.id.fontsize_data)
		fontSizeView.text = (appData.fontSize * 100).toString() + "%"

		// Load saved preferences
		updateIntBar.progress = appData.updateInterval
		intervalView.text = updateIntBar.progress.toString()

		findViewById<SeekBar>(R.id.fontsize_bar).apply {
			progress = (appData.fontSize * 20).toInt()

			setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
				override fun onStartTrackingTouch(seekBar: SeekBar?) {}
				override fun onStopTrackingTouch(seekBar: SeekBar?) {}
				override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
					val value = progress * 5
					fontSizeView.text = "$value%"
					appData.fontSize = value / 100.0f
				}
			})
		}



		updateIntBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				intervalView.text = progress.toString()
				appData.updateInterval = progress
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
		findViewById<Button>(R.id.button_theme).setOnClickListener {
			showThemeOptions(this)
		}
		findViewById<Button>(R.id.button_mode).setOnClickListener {
			showDarkModeOptions(this)
		}
	}

	private val handler = Handler(Looper.getMainLooper())

	private fun showDarkModeOptions(context: Context) {
		val options = arrayOf("Follow system", "Force Light", "Force Dark")

		val builder = AlertDialog.Builder(context)
		builder.setTitle("Select Dark Mode Option")
		builder.setItems(options) {dialog, which ->
			when (which) {
				0 -> {
					// Follow System
					appData.darkMode = 0
				}

				1 -> {
					// Force Light
					appData.darkMode = 2
				}

				2 -> {
					// Force Dark
					appData.darkMode = 1
				}
			}
		}
		builder.show()
	}

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

	private fun showThemeOptions(context: Context) {
		val themes = arrayOf("Blue", "Pink", "Purple", "Magenta", "Red", "Orange", "Yellow", "Green")

		val builder = AlertDialog.Builder(context)
		builder.setTitle("Select Theme")

		builder.setItems(themes) {_, which ->
			val selectedTheme = themes[which]

			// Save the selected theme ID in appData
			val themeId = when (selectedTheme) {
				"Blue" -> 1
				"Pink" -> 2
				"Purple" -> 3
				"Magenta" -> 4
				"Red" -> 5
				"Orange" -> 6
				"Yellow" -> 7
				"Green" -> 8
				else -> 0 // Default theme ID
			}
			appData.theme = themeId
		}

		builder.show()
	}

	private fun waitUntilBackgroundsLoaded(callback: () -> Unit) {
		if (!backgroundsLoaded) {
			handler.postDelayed({
				waitUntilBackgroundsLoaded(callback)
			}, 200) // Wait for 200ms
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
}