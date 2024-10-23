package com.example.projectuas

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.RecyclerView

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private val notesList: MutableList<NoteItem> = mutableListOf()
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        // Initialize components
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)
        fabAdd = findViewById(R.id.fabAdd)

        // Set up RecyclerView
        notesAdapter = NotesAdapter(notesList)
        recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        recyclerViewNotes.adapter = notesAdapter

        // Floating action button click listener to add new note item
        fabAdd.setOnClickListener {
            showAddNoteDialog()
        }
    }

    private fun showAddNoteDialog() {
        // Create a dialog to input text or select note type (text, checklist, file, etc.)
        val dialog = AddNoteDialogFragment { newNote ->
            notesList.add(newNote)
            notesAdapter.notifyDataSetChanged()
        }
        dialog.show(supportFragmentManager, "AddNoteDialog")
    }
}