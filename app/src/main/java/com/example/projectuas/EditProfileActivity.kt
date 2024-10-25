package com.example.projectuas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.projectuas.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

private val PICK_IMAGE_REQUEST = 1
private var imageUri: Uri? = null

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editProfileName: EditText
    private lateinit var auth: FirebaseAuth
    private val CAMERA_REQUEST_CODE = 2
    private val STORAGE_REQUEST_CODE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()

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

        // Ambil referensi ImageView untuk gambar profil
        val editProfileImageIcon: ImageView = findViewById(R.id.editProfileImageIcon)
        editProfileImageIcon.setOnClickListener {
            checkCameraPermissionsAndOpenCamera()
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
    private fun checkCameraPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_REQUEST_CODE
            )
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Permission denied to access camera", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Fungsi untuk membuka kamera
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val bitmap = data.extras?.get("data") as Bitmap
            val profileImageView: ImageView = findViewById(R.id.editProfileImage)
            profileImageView.setImageBitmap(bitmap) // Tampilkan gambar di ImageView

            // Simpan gambar ke Firebase Storage
            saveImageToFirebase(bitmap)
        }
    }

    // Fungsi untuk menyimpan gambar ke Firebase Storage
    private fun saveImageToFirebase(bitmap: Bitmap) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/${auth.currentUser?.uid}.jpg")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("profileImageUri", uri.toString())
                    apply()
                }
                Toast.makeText(this, "Profile image saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            // Tambahkan logging kesalahan
            Log.e("EditProfileActivity", "Failed to upload image: ${exception.message}")
            Toast.makeText(this, "Failed to save image: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

}