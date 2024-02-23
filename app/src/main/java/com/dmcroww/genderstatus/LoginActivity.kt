package com.dmcroww.genderstatus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dmcroww.genderstatus.providers.ApiClient

class LoginActivity: AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.act_login)
		val apiClient = ApiClient(applicationContext)
		val loginButton = findViewById<Button>(R.id.loginButton)
		val usernameInput = findViewById<EditText>(R.id.username)
		val passwordInput = findViewById<EditText>(R.id.password)

		loginButton.setOnClickListener {
			// Perform login here
			val username = usernameInput.text.toString()
			val password = passwordInput.text.toString()

			apiClient.login(username, password) {success, errorMessage ->
				if (!success) {
					// Login failed, present user with login screen
					Log.d("LOGIN", "Error: $errorMessage")
					Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
				} else {
					Log.d("LOGIN", "Login success.")
					// Login successful, navigate to main activity
					startActivity(Intent(this, MainActivity::class.java))
					finish()
				}
			}
		}
	}
}
