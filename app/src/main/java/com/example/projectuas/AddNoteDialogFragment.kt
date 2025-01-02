package com.example.projectuas

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import android.Manifest
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File

class AddNoteDialogFragment(private val onNoteAdded: (NoteItem) -> Unit) : DialogFragment() {

    private lateinit var etNoteContent: EditText
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var audioFile: File
    private lateinit var checklistContainer: LinearLayout
    private var currentImageUri: Uri? = null
    private var currentVideoUri: Uri? = null
    private var currentAudioUri: Uri? = null
    private var currentFileUri: Uri? = null
    private var photoFile: File? = null
    private var videoFile: File? = null

    private val storageReference: StorageReference by lazy {
        FirebaseStorage.getInstance().reference
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.fragment_add_note_dialog, null)
            checklistContainer = view.findViewById(R.id.checklistContainer)

            etNoteContent = view.findViewById(R.id.etNoteContent)
            val icToDoList: ImageView = view.findViewById(R.id.icToDoList)
            val icAddImage: ImageView = view.findViewById(R.id.icAddImage)
            val icAddVideo: ImageView = view.findViewById(R.id.icAddVideo)
            val icAddAudio: ImageView = view.findViewById(R.id.icAddAudio)
            val icAddFile: ImageView = view.findViewById(R.id.icAddFile)

            icToDoList.setOnClickListener {
                addChecklistItem()
            }

            icAddImage.setOnClickListener {
                showImagePickerDialog()
            }

            icAddVideo.setOnClickListener {
                showVideoPickerDialog()
            }

            icAddAudio.setOnClickListener {
                showAudioPickerDialog()
            }

            icAddFile.setOnClickListener {
                showFilePicker()
            }

            builder.setView(view)
                .setTitle("Add New Note")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton("Add") { dialog, _ ->
                    // Listener default diabaikan
                }

            val alertDialog = builder.create()

            // Mengganti listener default dengan custom
            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    coroutineScope.launch {
                        val noteDescription = etNoteContent.text.toString().trim()
                        val checklistItems = mutableListOf<ChecklistItem>()

                        // Mengumpulkan checklist items jika ada
                        for (i in 0 until checklistContainer.childCount) {
                            val checklistView = checklistContainer.getChildAt(i) as LinearLayout
                            val editText = checklistView.findViewById<EditText>(R.id.etChecklistItem)
                            editText?.text?.toString()?.trim()?.let {
                                if (it.isNotEmpty()) checklistItems.add(ChecklistItem(description = it))
                            }
                        }

                        // Validasi: setidaknya satu jenis konten harus diisi
                        if (noteDescription.isEmpty() &&
                            checklistItems.isEmpty() &&
                            currentImageUri == null &&
                            currentVideoUri == null &&
                            currentAudioUri == null &&
                            currentFileUri == null) {
                            Toast.makeText(requireContext(), "Please add at least one type of content", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // Simpan note dan update UI
                        saveNoteToFirestore(noteDescription, checklistItems)
                    }
                }
            }

            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun addChecklistItem() {
        val checklistItemView = layoutInflater.inflate(R.layout.item_checklist, checklistContainer, false)
        val removeButton = checklistItemView.findViewById<ImageButton>(R.id.btnRemoveChecklistItem)

        removeButton.setOnClickListener {
            checklistContainer.removeView(checklistItemView)
            Toast.makeText(requireContext(), "Checklist item removed", Toast.LENGTH_SHORT).show()
        }

        checklistContainer.addView(checklistItemView)
    }

    private fun createImageFile(): File {
        val timeStamp = System.currentTimeMillis()
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    private fun createVideoFile(): File {
        val timeStamp = System.currentTimeMillis()
        val videoFileName = "VIDEO_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile(
            videoFileName, /* prefix */
            ".mp4", /* suffix */
            storageDir /* directory */
        )
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Gallery", "Camera")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> captureImageFromCamera()
                }
            }
            .show()
    }

    private fun showVideoPickerDialog() {
        val options = arrayOf("Gallery", "Camera")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Video Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickVideoFromGallery()
                    1 -> captureVideoFromCamera()
                }
            }
            .show()
    }

    private fun showAudioPickerDialog() {
        val options = arrayOf("Device", "Record")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Audio Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickAudioFromDevice()
                    1 -> recordAudio()
                }
            }
            .show()
    }

    private fun showFilePicker() {
        val documentMimeTypes = arrayOf(
            "text/plain",                // TXT
            "application/pdf",           // PDF
            "application/msword",        // DOC
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
            "application/vnd.ms-excel",  // XLS
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",       // XLSX
            "application/vnd.ms-powerpoint", // PPT
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // PPTX
            "application/rtf",           // RTF
            "application/vnd.oasis.opendocument.text", // ODT
            "text/csv",                  // CSV
            "application/x-tex"          // LaTeX
        )
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, documentMimeTypes)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_FILE)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_VIDEO_GALLERY)
    }

    private fun pickAudioFromDevice() {
        val audioMimeTypes = arrayOf(
            "audio/mpeg",   // MP3
            "audio/wav",    // WAV
            "audio/ogg",    // OGG
            "audio/mp4",    // M4A
            "audio/aac"     // AAC
        )
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_MIME_TYPES, audioMimeTypes)
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(Intent.createChooser(intent, "Select Audio"), REQUEST_AUDIO_DEVICE)
    }

    private fun captureImageFromCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                try {
                    photoFile = createImageFile()
                    photoFile?.let { file ->
                        val photoURI: Uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(intent, REQUEST_IMAGE_CAMERA)
                    }
                } catch (ex: Exception) {
                    Toast.makeText(requireContext(), "Error creating image file: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun captureVideoFromCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                try {
                    videoFile = createVideoFile()
                    videoFile?.let { file ->
                        val videoURI: Uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        startActivityForResult(intent, REQUEST_VIDEO_CAMERA)
                    }
                } catch (ex: Exception) {
                    Toast.makeText(requireContext(), "Error creating video file: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun recordAudio() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }

        try {
            audioFile = File(requireContext().getExternalFilesDir(null), "audio_record_${System.currentTimeMillis()}.mp3")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            Toast.makeText(requireContext(), "Rekaman dimulai", Toast.LENGTH_SHORT).show()

            AlertDialog.Builder(requireContext())
                .setTitle("Rekam Audio")
                .setMessage("Sedang merekam...")
                .setPositiveButton("Berhenti") { _, _ ->
                    stopRecording()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal merekam: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder.stop()
            mediaRecorder.release()
            Toast.makeText(requireContext(), "Rekaman selesai", Toast.LENGTH_SHORT).show()

            // Tambahkan file audio ke note dengan content URI
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", audioFile)
            currentAudioUri = uri

            // Biarkan pengguna menekan tombol "Add" untuk menyimpan note
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Anda perlu menentukan apakah ingin memutar kamera untuk gambar atau video
                    // Bisa dengan menyimpan state sebelumnya atau meminta pengguna untuk memilih lagi
                    Toast.makeText(requireContext(), "Permission granted, please retry the action", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recordAudio()
                } else {
                    Toast.makeText(requireContext(), "Audio permission is required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_GALLERY -> {
                    val uri: Uri? = data?.data
                    currentImageUri = uri
                    Toast.makeText(requireContext(), "Image added from gallery", Toast.LENGTH_SHORT).show()
                }
                REQUEST_IMAGE_CAMERA -> {
                    photoFile?.let { file ->
                        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
                        currentImageUri = uri
                        Toast.makeText(requireContext(), "Image captured from camera", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(requireContext(), "Error accessing captured image", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_VIDEO_GALLERY -> {
                    val uri: Uri? = data?.data
                    currentVideoUri = uri
                    if (uri != null) {
                        try {
                            val takeFlags: Int = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                        } catch (e: SecurityException) {
                            Toast.makeText(requireContext(), "Tidak dapat mengambil izin file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        Toast.makeText(requireContext(), "Video ditambahkan dari gallery", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Gagal menambahkan video", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_VIDEO_CAMERA -> {
                    val uri: Uri? = videoFile?.let { file ->
                        FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
                    }
                    currentVideoUri = uri
                    if (uri != null) {
                        Toast.makeText(requireContext(), "Video ditambahkan dari camera", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Gagal menambahkan video", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_AUDIO_DEVICE -> { // Handling audio dari device
                    val uri: Uri? = data?.data
                    currentAudioUri = uri
                    if (uri != null) {
                        // Ambil izin persistable URI permission
                        try {
                            val takeFlags: Int = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                        } catch (e: SecurityException) {
                            Toast.makeText(requireContext(), "Tidak dapat mengambil izin file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                        Toast.makeText(requireContext(), "Audio added from device", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add audio", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_FILE -> {
                    val uri: Uri? = data?.data
                    if (uri != null) {
                        try {
                            // Ambil persistent permission
                            val takeFlags: Int = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

                            // Set URI
                            currentFileUri = uri

                            // Get filename
                            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                            cursor?.use { c ->
                                if (c.moveToFirst()) {
                                    val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (displayNameIndex != -1) {
                                        val originalFileName = c.getString(displayNameIndex)
                                        Toast.makeText(requireContext(), "File added: $originalFileName", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            Toast.makeText(requireContext(), "Error accessing file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun saveNoteToFirestore(noteDescription: String, checklistItems: List<ChecklistItem>) {
        val projectId = arguments?.getString("projectId") ?: return
        val taskId = arguments?.getString("taskId") ?: return
        val firestore = FirebaseFirestore.getInstance()

        val noteData = hashMapOf(
            "content" to noteDescription,
            "isChecklist" to checklistItems.isNotEmpty(),
            "checklistItems" to checklistItems.map {
                mapOf(
                    "description" to it.description,
                    "isChecked" to it.isChecked
                )
            },
            "timestamp" to System.currentTimeMillis()
        )

        // Fungsi rekursif untuk mengunggah semua media
        fun uploadMedia(mediaList: List<Pair<Uri?, String>>, currentIndex: Int) {
            if (currentIndex >= mediaList.size) {
                // Semua media telah diunggah, simpan note ke Firestore
                firestore.collection("projects")
                    .document(projectId)
                    .collection("taskList")
                    .document(taskId)
                    .collection("notes")
                    .add(noteData)
                    .addOnSuccessListener { documentReference ->
                        val noteItem = NoteItem(
                            content = noteDescription,
                            isChecklist = checklistItems.isNotEmpty(),
                            checklistItems = checklistItems,
                            isImage = noteData["imageUri"] != null,
                            isVideo = noteData["videoUri"] != null,
                            isAudio = noteData["audioUrl"] != null,
                            isFile = noteData["fileUri"] != null,
                            fileName = noteData["fileName"] as? String,
                            uri = noteData["imageUri"]?.let { Uri.parse(it as String) }
                                ?: noteData["videoUri"]?.let { Uri.parse(it as String) }
                                ?: noteData["fileUri"]?.let { Uri.parse(it as String) },
                            audioUrl = noteData["audioUrl"]?.let { Uri.parse(it as String) },
                            videoThumbnailUri = noteData["videoThumbnailUri"] as? String // Set thumbnail URI
                        )
                        onNoteAdded(noteItem)
                        dialog?.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal menyimpan note: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                return
            }

            val (currentUri, mediaType) = mediaList[currentIndex]
            if (currentUri == null) {
                // Lanjut ke media berikutnya jika current URI null
                uploadMedia(mediaList, currentIndex + 1)
                return
            }

            if (mediaType == "videos") {
                // Upload video terlebih dahulu
                coroutineScope.launch {
                    try {
                        val downloadUrl = uploadMediaToFirebase(currentUri, mediaType)
                        if (downloadUrl != null) {
                            noteData["videoUri"] = downloadUrl

                            // Generate thumbnail
                            val thumbnail = generateVideoThumbnail(currentUri)
                            if (thumbnail != null) {
                                // Upload thumbnail
                                val thumbnailUrl = uploadThumbnailToFirebase(thumbnail, currentUri)
                                if (thumbnailUrl != null) {
                                    noteData["videoThumbnailUri"] = thumbnailUrl
                                } else {
                                    Toast.makeText(requireContext(), "Failed to upload video thumbnail", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed to generate video thumbnail", Toast.LENGTH_SHORT).show()
                            }

                            // Lanjut ke media berikutnya
                            uploadMedia(mediaList, currentIndex + 1)
                        } else {
                            Toast.makeText(requireContext(), "Failed to upload video", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Gagal mengunggah media: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Untuk jenis media lainnya
                coroutineScope.launch {
                    try {
                        val downloadUrl = uploadMediaToFirebase(currentUri, mediaType)
                        if (downloadUrl != null) {
                            when (mediaType) {
                                "images" -> noteData["imageUri"] = downloadUrl
                                "audio" -> noteData["audioUrl"] = downloadUrl
                                "files" -> {
                                    noteData["fileUri"] = downloadUrl
                                    currentUri.let { uri ->
                                        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                                        cursor?.use { c ->
                                            if (c.moveToFirst()) {
                                                val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                                if (displayNameIndex != -1) {
                                                    noteData["fileName"] = c.getString(displayNameIndex)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Lanjut ke media berikutnya
                            uploadMedia(mediaList, currentIndex + 1)
                        } else {
                            Toast.makeText(requireContext(), "Failed to upload media", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Gagal mengunggah media: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Mulai proses upload dengan daftar semua media
        val mediaList = listOf(
            Pair(currentImageUri, "images"),
            Pair(currentVideoUri, "videos"),
            Pair(currentAudioUri, "audio"),
            Pair(currentFileUri, "files")
        )
        uploadMedia(mediaList, 0)
    }

    private suspend fun uploadMediaToFirebase(uri: Uri, mediaType: String): String? {
        return try {
            val timeStamp = System.currentTimeMillis()
            val fileName = "$mediaType/${timeStamp}_${uri.lastPathSegment}"
            val mediaRef = storageReference.child(fileName)

            mediaRef.putFile(uri).await()
            val downloadUrl = mediaRef.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun generateVideoThumbnail(uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(requireContext(), uri)
            val bitmap = retriever.frameAtTime
            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun uploadThumbnailToFirebase(bitmap: Bitmap, videoUri: Uri): String? {
        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            val thumbnailFileName = "thumbnails/${System.currentTimeMillis()}_${videoUri.lastPathSegment}.jpg"
            val thumbnailRef = storageReference.child(thumbnailFileName)
            thumbnailRef.putBytes(data).await()

            val downloadUrl = thumbnailRef.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    companion object {
        private const val REQUEST_FILE = 1
        private const val REQUEST_IMAGE_GALLERY = 2
        private const val REQUEST_VIDEO_GALLERY = 3
        private const val REQUEST_VIDEO_CAMERA = 4
        private const val REQUEST_AUDIO_DEVICE = 6
        private const val REQUEST_IMAGE_CAMERA = 8
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 9
    }
}
