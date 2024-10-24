package com.example.projectuas

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectuas.R

private val PICK_IMAGE_REQUEST = 1
private var imageUri: Uri? = null

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editProfileName: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Back Button Logic
        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()  // Mengakhiri aktivitas dan kembali ke halaman sebelumnya
        }

        // Ambil referensi dari EditText untuk nama
        editProfileName = findViewById(R.id.editProfileName)

        // Set nama awal dari SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val currentName = sharedPref.getString("username", "")
        editProfileName.setText(currentName)

        // Ganti Foto Profil
        val editProfileImageIcon: ImageView = findViewById(R.id.editProfileImageIcon)
        editProfileImageIcon.setOnClickListener {
            openGallery()
        }

        // Save Button Logic
        val saveButton: Button = findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        // Ambil nama baru dari EditText
        val newName = editProfileName.text.toString()

        // Simpan nama baru ke SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", newName)
            apply()
        }

        // Menampilkan pesan bahwa profil telah disimpan
        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

        // Kembali ke halaman Profile
        finish()
    }

    // Buka galeri untuk memilih gambar
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Setelah gambar dipilih, simpan URI dan tampilkan di ImageView
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.data
            val profileImageView: ImageView = findViewById(R.id.editProfileImage)
            profileImageView.setImageURI(imageUri)  // Tampilkan gambar di ImageView

            // Simpan URI ke SharedPreferences atau database jika diperlukan
            val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("profileImageUri", imageUri.toString())
                apply()
            }
        }
    }
}