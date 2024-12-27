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
import android.app.Activity
import android.media.MediaRecorder
import android.util.Log
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

                    for (i in 0 until checklistContainer.childCount) {
                        val checklistView = checklistContainer.getChildAt(i) as LinearLayout
                        val editText = checklistView.findViewById<EditText>(R.id.etChecklistItem)
                        editText?.text?.toString()?.trim()?.let {
                            if (it.isNotEmpty()) checklistItems.add(ChecklistItem(description = it))
                        }
                    }

                    if (checklistItems.isNotEmpty() || noteDescription.isNotEmpty()) {
                        val note = NoteItem(
                            content = noteDescription,
                            isChecklist = checklistItems.isNotEmpty(),
                            checklistItems = checklistItems
                        )
                        saveNoteToFirebase(note)
                        // Jangan tambahkan catatan ke UI di sini
                    } else {
                        Toast.makeText(requireContext(), "Please add a note or checklist", Toast.LENGTH_SHORT).show()
                    }
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


    private fun saveNoteToFirebase(note: NoteItem) {
        val projectId = arguments?.getString("projectId") ?: return
        val taskId = arguments?.getString("taskId") ?: return

        // Tentukan tipe catatan
        val type = when {
            note.isChecklist -> "checklist"
            note.isImage -> "image"
            note.isVideo -> "video"
            note.isAudio -> "audio"
            note.isFile -> "file"
            else -> "text"
        }

        val noteData = hashMapOf(
            "content" to note.content,
            "type" to type,
            "isChecklist" to note.isChecklist,
            "checklistItems" to note.checklistItems.map { checklistItem ->
                mapOf(
                    "description" to checklistItem.description,
                    "isChecked" to checklistItem.isChecked
                )
            },
            "isImage" to note.isImage,
            "isVideo" to note.isVideo,
            "isAudio" to note.isAudio,
            "isFile" to note.isFile
        )


        // Jika ada URI, tambahkan juga
        note.uri?.let {
            noteData["uri"] = it.toString()
        }

        FirebaseFirestore.getInstance()
            .collection("projects")
            .document(projectId)
            .collection("taskList")
            .document(taskId)
            .collection("notes")
            .add(noteData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Note saved successfully", Toast.LENGTH_SHORT).show()
                onNoteAdded(note)
                dialog?.dismiss()
            }
            .addOnFailureListener { e ->
                Log.e("AddNoteDialogFragment", "Error saving note", e)
                Toast.makeText(requireContext(), "Failed to save note", Toast.LENGTH_SHORT).show()
            }
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
                    val note = NoteItem(content = "Image Note", isImage = true, uri = uri)
                    saveNoteToFirebase(note)
                    onNoteAdded(note)
                }
                REQUEST_VIDEO_GALLERY, REQUEST_VIDEO_CAMERA -> {
                    val note = NoteItem(content = "Video Note", isVideo = true, uri = uri)
                    saveNoteToFirebase(note)
                    onNoteAdded(note)
                }
                REQUEST_AUDIO_DEVICE, REQUEST_AUDIO_RECORD -> {
                    val note = NoteItem(content = "Audio Note", isAudio = true, uri = uri)
                    saveNoteToFirebase(note)
                    onNoteAdded(note)
                }
                REQUEST_FILE -> {
                    val note = NoteItem(content = "File Note", isFile = true, uri = uri)
                    saveNoteToFirebase(note)
                    onNoteAdded(note)
                }
                else -> {
                    // Default text note
                    val noteContent = etNoteContent.text.toString()
                    val note = NoteItem(content = noteContent)
                    saveNoteToFirebase(note)
                    onNoteAdded(note)
                }
            }
            dismiss()
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(requireContext(), "Action canceled", Toast.LENGTH_SHORT).show()
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