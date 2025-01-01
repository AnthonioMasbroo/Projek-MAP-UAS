package com.example.projectuas

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

private const val PICK_IMAGE_REQUEST = 1
private const val CAMERA_REQUEST_CODE = 2
private const val STORAGE_REQUEST_CODE = 3

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editProfileName: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var sharedPref: android.content.SharedPreferences
    private var imageUri: Uri? = null
    private lateinit var editProfileEmail: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var saveButton: Button
    private var isPasswordVisible = false
    private lateinit var ivToggleNewPassword: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Inisialisasi View
        editProfileName = findViewById(R.id.editProfileName)
        val profileImageView: ImageView = findViewById(R.id.editProfileImage)
        val editProfileImageIcon: ImageView = findViewById(R.id.editProfileImageIcon)
        val backButton: ImageView = findViewById(R.id.backButton)
        saveButton = findViewById(R.id.saveButton)
        editProfileEmail = findViewById(R.id.editProfileEmail)
        editNewPassword = findViewById(R.id.editNewPassword)
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword)

        // Set nama awal dari SharedPreferences
        val currentName = sharedPref.getString("username", "")
        editProfileName.setText(currentName)

        // Set email awal dari SharedPreferences
        val currentEmail = sharedPref.getString("email", "")
        editProfileEmail.setText(currentEmail)

        // Setup toggle password visibility
        ivToggleNewPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            // Toggle password visibility
            if (isPasswordVisible) {
                // Show password
                editNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivToggleNewPassword.setImageResource(R.drawable.baseline_visibility_24)
            } else {
                // Hide password
                editNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivToggleNewPassword.setImageResource(R.drawable.baseline_visibility_off_24)
            }
            // Maintain cursor position
            editNewPassword.setSelection(editNewPassword.text.length)
        }

        // Tampilkan gambar profil jika tersedia
        val profileImageUrl = sharedPref.getString("profileImageUri", null)
        profileImageUrl?.let {
            Glide.with(this)
                .load(it)
                .circleCrop() // Membuat gambar menjadi bulat
                .into(profileImageView)
        }

        // Inisialisasi ikon edit gambar profil
        editProfileImageIcon.setOnClickListener {
            openGalleryToPickImage()
        }

        // Tombol Kembali
        backButton.setOnClickListener {
            finish()  // Mengakhiri aktivitas dan kembali ke halaman sebelumnya
        }

        // Save Button Logic
        saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        // Ambil nilai dari input fields
        val newName = editProfileName.text.toString().trim()
        val newEmail = editProfileEmail.text.toString().trim()
        val newPassword = editNewPassword.text.toString().trim()

        // Validasi input
        if (!validateInput(newName, newEmail, newPassword)) {
            return
        }

        // Show loading state
        showLoading(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            return
        }

        // Mulai proses update
        val currentUser = auth.currentUser
        val updates = mutableListOf<(onComplete: (Boolean) -> Unit) -> Unit>()

        // Update email jika berubah
        if (newEmail != currentUser?.email) {
            updates.add { onComplete ->
                currentUser?.verifyBeforeUpdateEmail(newEmail)
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Verifikasi email telah dikirim ke $newEmail", Toast.LENGTH_LONG).show()
                            onComplete(true)
                        } else {
                            Toast.makeText(this, "Gagal mengupdate email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                    }
            }
        }

        // Update password jika diisi
        if (newPassword.isNotEmpty()) {
            updates.add { onComplete ->
                currentUser?.updatePassword(newPassword)
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            onComplete(true)
                        } else {
                            Toast.makeText(this, "Gagal mengupdate password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                    }
            }
        }

        // Update profil di Firestore dan SharedPreferences
        updates.add { onComplete ->
            val userUpdates = hashMapOf<String, Any>(
                "username" to newName,
                "email" to newEmail,
                "lastUpdated" to System.currentTimeMillis()
            )

            firestore.collection("users").document(userId)
                .update(userUpdates)
                .addOnSuccessListener {
                    // Update SharedPreferences
                    with(sharedPref.edit()) {
                        putString("username", newName)
                        putString("email", newEmail)
                        apply()
                    }
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal memperbarui profil: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
        }

        // Eksekusi semua updates secara berurutan
        executeUpdatesSequentially(updates) { allSuccessful ->
            showLoading(false)
            if (allSuccessful) {
                Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    // Fungsi helper untuk validasi input
    private fun validateInput(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            editProfileName.error = "Nama tidak boleh kosong"
            return false
        }

        if (email.isEmpty()) {
            editProfileEmail.error = "Email tidak boleh kosong"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editProfileEmail.error = "Format email tidak valid"
            return false
        }

        if (password.isNotEmpty() && password.length < 6) {
            editNewPassword.error = "Password minimal 6 karakter"
            return false
        }

        return true
    }

    // Fungsi helper untuk menjalankan updates secara berurutan
    private fun executeUpdatesSequentially(
        updates: List<(onComplete: (Boolean) -> Unit) -> Unit>,
        onAllComplete: (Boolean) -> Unit
    ) {
        if (updates.isEmpty()) {
            onAllComplete(true)
            return
        }

        var currentIndex = 0
        fun executeNext() {
            if (currentIndex >= updates.size) {
                onAllComplete(true)
                return
            }

            updates[currentIndex] { success ->
                if (success) {
                    currentIndex++
                    executeNext()
                } else {
                    onAllComplete(false)
                }
            }
        }

        executeNext()
    }

    // Fungsi helper untuk menampilkan/menyembunyikan loading state
    private fun showLoading(show: Boolean) {
        // Implementasi loading indicator (ProgressBar atau LoadingDialog)
        if (show) {
            // Show loading indicator
            saveButton.isEnabled = false
            saveButton.text = "Menyimpan..."
        } else {
            // Hide loading indicator
            saveButton.isEnabled = true
            saveButton.text = "Save"
        }
    }


    // Buka galeri untuk memilih gambar
    private fun openGalleryToPickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

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
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                // Periksa apakah pengguna telah menolak izin secara permanen
                val cameraPermissionDenied = !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                val storagePermissionDenied = !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

                if (cameraPermissionDenied || storagePermissionDenied) {
                    // Pengguna telah menolak izin secara permanen
                    showPermissionDeniedDialog()
                } else {
                    // Pengguna telah menolak izin tanpa memilih "Don't ask again"
                    Toast.makeText(this, "Izin ditolak untuk mengakses kamera dan penyimpanan", Toast.LENGTH_SHORT).show()
                }
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
        val profileImageView: ImageView = findViewById(R.id.editProfileImage)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val bitmap = data.extras?.get("data") as Bitmap
            profileImageView.setImageBitmap(bitmap) // Tampilkan gambar di ImageView

            // Simpan gambar ke Firebase Storage
            saveImageToFirebase(bitmap)
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            selectedImageUri?.let {
                profileImageView.setImageURI(it)
                uploadImageToFirebase(it)
            }
        }
    }

    // Fungsi untuk menyimpan gambar ke Firebase Storage
    private fun saveImageToFirebase(bitmap: Bitmap) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = storage.reference.child("profile_images/$userId.jpg")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Simpan URL ke SharedPreferences dan Firestore
                    with(sharedPref.edit()) {
                        putString("profileImageUri", uri.toString())
                        apply()
                    }

                    firestore.collection("users").document(userId)
                        .update("profileImageUrl", uri.toString())
                        .addOnSuccessListener {
                            // Tampilkan gambar dengan Glide dan circular crop
                            Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .into(findViewById(R.id.editProfileImage))

                            Toast.makeText(this, "Gambar profil diperbarui", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal menyimpan gambar profil: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EditProfileActivity", "Gagal mengupload gambar: ${exception.message}")
                Toast.makeText(this, "Gagal menyimpan gambar: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Fungsi untuk mengupload gambar dari URI ke Firebase Storage
    private fun uploadImageToFirebase(uri: Uri) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = storage.reference.child("profile_images/$userId.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Simpan URL ke SharedPreferences dan Firestore
                    with(sharedPref.edit()) {
                        putString("profileImageUri", downloadUri.toString())
                        apply()
                    }

                    firestore.collection("users").document(userId)
                        .update("profileImageUrl", downloadUri.toString())
                        .addOnSuccessListener {
                            // Tampilkan gambar dengan Glide dan circular crop
                            Glide.with(this)
                                .load(downloadUri)
                                .circleCrop()
                                .into(findViewById(R.id.editProfileImage))

                            Toast.makeText(this, "Gambar profil diperbarui", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal menyimpan gambar profil: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EditProfileActivity", "Gagal mengupload gambar: ${exception.message}")
                Toast.makeText(this, "Gagal menyimpan gambar: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Fungsi untuk membuka pengaturan aplikasi
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    // Fungsi untuk menampilkan dialog penolakan izin permanen
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Izin Diperlukan")
            .setMessage("Aplikasi ini memerlukan izin kamera dan penyimpanan untuk memilih gambar profil. Silakan aktifkan izin tersebut di pengaturan aplikasi.")
            .setPositiveButton("Buka Pengaturan") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Tidak dapat memilih gambar tanpa izin", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }
}
