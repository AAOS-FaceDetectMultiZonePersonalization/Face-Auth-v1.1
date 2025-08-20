package com.example.tf_face

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.ImageButton
import android.widget.ArrayAdapter
import android.widget.Spinner

class NewUserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_new_user)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val ageInput = findViewById<EditText>(R.id.ageInput)
        val weightInput = findViewById<EditText>(R.id.weightInput)
        val continueButton = findViewById<Button>(R.id.btnContinue)
        val genderSpinner = findViewById<Spinner>(R.id.genderSpinner)

        // Set up gender spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.gender_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genderSpinner.adapter = adapter
        }

        val backButton = findViewById<ImageButton>(R.id.fabBack)
        backButton?.setOnClickListener {
            // Go back to RecognitionActivity instead of finishing
            startActivity(Intent(this, RecognitionActivity::class.java))
            finish()
        }

        continueButton?.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val age = ageInput.text.toString().trim()
            val gender = genderSpinner.selectedItem.toString()
            val weight = weightInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (age.isEmpty()) {
                Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (gender.isEmpty()) {
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (weight.isEmpty()) {
                Toast.makeText(this, "Please enter your weight", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Start frame capture activity with user details
            val intent = Intent(this, CaptureFramesActivity::class.java).apply {
                putExtra("user_name", name)
                putExtra("user_age", age.toInt())
                putExtra("user_gender", gender)
                putExtra("user_weight", weight.toFloat())
            }
            startActivity(intent)
            finish()
        }
    }
}