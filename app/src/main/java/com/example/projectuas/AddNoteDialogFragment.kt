package com.example.projectuas

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.widget.EditText

class AddNoteDialogFragment(private val onNoteAdded: (NoteItem) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val input = EditText(it)
            input.hint = "Type your note here"

            builder.setView(input)
                .setTitle("Add New Note")
                .setPositiveButton("Add") { dialog, id ->
                    val noteContent = input.text.toString()
                    if (noteContent.isNotEmpty()) {
                        val note = NoteItem(noteContent, isChecklist = false) // Change isChecklist to true if checkbox
                        onNoteAdded(note)
                    }
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}