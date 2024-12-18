package com.example.projectuas

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import android.Manifest
import android.app.Activity

class AddNoteDialogFragment(private val onNoteAdded: (NoteItem) -> Unit) : DialogFragment() {

    private lateinit var etNoteContent: EditText
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.fragment_add_note_dialog, null)

            val etNoteContent: EditText = view.findViewById(R.id.etNoteContent)
            val icToDoList: ImageView = view.findViewById(R.id.icToDoList)
            val icAddImage: ImageView = view.findViewById(R.id.icAddImage)
            val icAddVideo: ImageView = view.findViewById(R.id.icAddVideo)
            val icAddAudio: ImageView = view.findViewById(R.id.icAddAudio)
            val icAddFile: ImageView = view.findViewById(R.id.icAddFile)

            icToDoList.setOnClickListener {
                val checklist = mutableListOf<ChecklistItem>()
                val items = etNoteContent.text.toString().split("\n") // Setiap baris sebagai item
                for (item in items) {
                    if (item.isNotBlank()) {
                        checklist.add(ChecklistItem(description = item.trim()))
                    }
                }
                if (checklist.isNotEmpty()) {
                    val note = NoteItem(content = "Checklist", isChecklist = true, checklistItems = checklist)
                    onNoteAdded(note)
                    dismiss()
                }
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
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
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
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> pickAudioFromDevice()
                    1 -> recordAudio()
                }
            }
            .show()
    }

    private fun showFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
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
        fun captureVideoFromCamera() {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                startActivityForResult(intent, REQUEST_VIDEO_CAPTURE)
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }

    }

    private fun captureVideoFromCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Izin telah diberikan
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            startActivityForResult(intent, REQUEST_VIDEO_CAMERA)
        } else {
            // Minta izin
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun pickAudioFromDevice() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_AUDIO_DEVICE)
    }

    private fun recordAudio() {
        val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
        startActivityForResult(intent, REQUEST_AUDIO_RECORD)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan
                captureVideoFromCamera()
            } else {
                // Izin ditolak
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val noteContent = etNoteContent.text.toString()
            val uri: Uri? = data.data
            when (requestCode) {
                REQUEST_IMAGE_GALLERY, REQUEST_IMAGE_CAMERA -> {
                    val note = NoteItem(noteContent, isChecklist = false, isImage = true, uri = uri)
                    onNoteAdded(note)
                }
                REQUEST_VIDEO_GALLERY, REQUEST_VIDEO_CAMERA -> {
                    val note = NoteItem(noteContent, isChecklist = false, isVideo = true, uri = uri)
                    onNoteAdded(note)
                }
                REQUEST_AUDIO_DEVICE, REQUEST_AUDIO_RECORD -> {
                    val note = NoteItem(noteContent, isChecklist = false, isAudio = true, uri = uri)
                    onNoteAdded(note)
                }
                REQUEST_FILE -> {
                    val note = NoteItem(noteContent, isChecklist = false, isFile = true, uri = uri)
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