package com.example.projectuas

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import android.Manifest
import android.media.MediaRecorder
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import com.google.firebase.firestore.FirebaseFirestore
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
                        return@setPositiveButton
                    }

                    // Buat note dengan konten yang ada
                    val note = NoteItem(
                        content = noteDescription,
                        isChecklist = checklistItems.isNotEmpty(),
                        checklistItems = checklistItems,
                        isImage = currentImageUri != null,
                        isVideo = currentVideoUri != null,
                        isAudio = currentAudioUri != null,
                        isFile = currentFileUri != null,
                        uri = when {
                            currentImageUri != null -> currentImageUri
                            currentVideoUri != null -> currentVideoUri
                            currentAudioUri != null -> currentAudioUri
                            currentFileUri != null -> currentFileUri
                            else -> null
                        }
                    )

                    // Simpan note
                    saveNoteToFirestore(note)
                }
            builder.create()
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

    private fun saveNoteToFirestore(note: NoteItem) {
        val projectId = arguments?.getString("projectId") ?: return
        val taskId = arguments?.getString("taskId") ?: return

        // Konversi checklist items ke format yang bisa disimpan di Firestore
        val checklistItemsForFirestore = note.checklistItems.map { item ->
            mapOf(
                "description" to item.description,
                "isChecked" to item.isChecked
            )
        }

        val noteData = hashMapOf(
            "content" to etNoteContent.text.toString(),
            "isChecklist" to true,
            "checklistItems" to checklistItemsForFirestore,
            "timestamp" to System.currentTimeMillis()
        )

        // Tambahkan media jika ada
        currentImageUri?.let { noteData["imageUri"] = it.toString() }
        currentVideoUri?.let { noteData["videoUri"] = it.toString() }
        currentAudioUri?.let { noteData["audioUri"] = it.toString() }
        currentFileUri?.let { noteData["fileUri"] = it.toString() }

        FirebaseFirestore.getInstance()
            .collection("projects")
            .document(projectId)
            .collection("taskList")
            .document(taskId)
            .collection("notes")
            .add(noteData)
            .addOnSuccessListener {
                // Buat checklist items untuk NoteItem
                val checklistItems = getChecklistItems().map { item ->
                    ChecklistItem(
                        description = item.text.toString(),
                        isChecked = false
                    )
                }

                val noteItem = NoteItem(
                    content = etNoteContent.text.toString(),
                    isChecklist = true,
                    checklistItems = checklistItems,
                    isImage = currentImageUri != null,
                    isVideo = currentVideoUri != null,
                    isAudio = currentAudioUri != null,
                    isFile = currentFileUri != null,
                    uri = currentImageUri ?: currentVideoUri ?: currentAudioUri ?: currentFileUri
                )
                onNoteAdded(noteItem)
                dialog?.dismiss()
            }
    }

    private fun getChecklistItems(): List<EditText> {
        val items = mutableListOf<EditText>()
        for (i in 0 until checklistContainer.childCount) {
            val checklistView = checklistContainer.getChildAt(i) as LinearLayout
            val editText = checklistView.findViewById<EditText>(R.id.etChecklistItem)
            if (!editText.text.isNullOrEmpty()) {
                items.add(editText)
            }
        }
        return items
    }

    fun removeChecklistItem(view: View) {
        val parent = view.parent as? LinearLayout
        if (parent != null) {
            checklistContainer.removeView(parent)
            Toast.makeText(requireContext(), "Checklist item removed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Error removing checklist item", Toast.LENGTH_SHORT).show()
        }
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
            "application/pdf",           // PDF
            "application/msword",        // DOC
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
            "application/vnd.ms-excel",  // XLS
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",       // XLSX
            "application/vnd.ms-powerpoint", // PPT
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" // PPTX
        )
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, documentMimeTypes)
        }
        startActivityForResult(intent, REQUEST_FILE)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_VIDEO_GALLERY)
    }

    private fun captureImageFromCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_IMAGE_CAMERA)
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
            startActivityForResult(intent, REQUEST_VIDEO_CAMERA)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun pickAudioFromDevice() {
        val audioMimeTypes = arrayOf(
            "audio/mpeg",   // MP3
            "audio/wav",    // WAV
            "audio/ogg",    // OGG
            "audio/mp4",    // M4A
            "audio/aac"     // AAC
        )
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).apply {
            type = "audio/*"
        }
        startActivityForResult(intent, REQUEST_AUDIO_DEVICE)
    }


    private fun recordAudio() {
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

            // Tambahkan file audio ke note
            val uri = Uri.fromFile(audioFile)
            val note = NoteItem(content = "Rekaman Audio", isChecklist = false, isAudio = true, uri = uri)
            onNoteAdded(note)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImageFromCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data
            when (requestCode) {
                REQUEST_IMAGE_GALLERY, REQUEST_IMAGE_CAMERA -> {
                    currentImageUri = uri
                    Toast.makeText(requireContext(), "Image added", Toast.LENGTH_SHORT).show()
                }
                REQUEST_VIDEO_GALLERY, REQUEST_VIDEO_CAMERA -> {
                    currentVideoUri = uri
                    Toast.makeText(requireContext(), "Video added", Toast.LENGTH_SHORT).show()
                }
                REQUEST_AUDIO_DEVICE, REQUEST_AUDIO_RECORD -> {
                    currentAudioUri = uri
                    Toast.makeText(requireContext(), "Audio added", Toast.LENGTH_SHORT).show()
                }
                REQUEST_FILE -> {
                    currentFileUri = uri
                    Toast.makeText(requireContext(), "File added", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_FILE = 1
        private const val REQUEST_IMAGE_GALLERY = 2
        private const val REQUEST_VIDEO_GALLERY = 3
        private const val REQUEST_VIDEO_CAMERA = 4
        private const val REQUEST_VIDEO_CAPTURE = 5
        private const val REQUEST_AUDIO_DEVICE = 6
        private const val REQUEST_AUDIO_RECORD = 7
        private const val REQUEST_IMAGE_CAMERA = 8
    }
}