package com.example.projectuas

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var notesAdapter: NotesAdapter
    private val notesList = mutableListOf<NoteItem>()
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvTaskTitle: TextView
    private lateinit var tvNoNotes: TextView
    private lateinit var taskName: String

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        // Inisialisasi views
        tvTaskTitle = findViewById(R.id.tvTaskTitle)
        tvNoNotes = findViewById(R.id.tvNoNotes) // Inisialisasi tvNoNotes
        notesAdapter = NotesAdapter(notesList)
        recyclerViewNotes = findViewById<RecyclerView>(R.id.recyclerViewNotes).apply {
            layoutManager = LinearLayoutManager(this@TaskDetailActivity)
            adapter = notesAdapter
        }

        fabAdd = findViewById(R.id.fabAdd)

        // Ambil data task dari intent
        taskName = intent.getStringExtra("taskName") ?: run {
            Toast.makeText(this, "Task name missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Tampilkan judul task
        tvTaskTitle.text = taskName

        fabAdd.setOnClickListener {
            showAddNoteDialog()
        }

        // Fetch notes untuk task ini
        fetchTaskNotes(taskName)
    }

    private fun showAddNoteDialog() {
        val dialog = AddNoteDialogFragment { newNote ->
            notesList.add(newNote)
            notesAdapter.notifyDataSetChanged()
            tvNoNotes.visibility = if (notesList.isEmpty()) View.VISIBLE else View.GONE
        }.apply {
            // Pass projectId dan taskId jika diperlukan
            arguments = Bundle().apply {
                putString("projectId", intent.getStringExtra("projectId"))
                putString("taskId", taskName) // Asumsi taskId adalah nama task
            }
        }
        dialog.show(supportFragmentManager, "AddNoteDialog")
    }

    private fun fetchTaskNotes(taskName: String) {
        val projectId = intent.getStringExtra("projectId") ?: return

        FirebaseFirestore.getInstance()
            .collection("projects")
            .document(projectId)
            .collection("taskList")
            .document(taskName)
            .collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { notesSnapshot ->
                notesList.clear()

                for (noteDoc in notesSnapshot.documents) {
                    val content = noteDoc.getString("content") ?: ""
                    val checklistItems = (noteDoc.get("checklistItems") as? List<Map<String, Any>>)?.map {
                        ChecklistItem(
                            description = it["description"] as? String ?: "",
                            isChecked = it["isChecked"] as? Boolean ?: false
                        )
                    } ?: listOf()

                    val imageUri = noteDoc.getString("imageUri")?.let { Uri.parse(it) }
                    val videoUri = noteDoc.getString("videoUri")?.let { Uri.parse(it) }
                    val audioUri = noteDoc.getString("audioUri")?.let { Uri.parse(it) }
                    val fileUri = noteDoc.getString("fileUri")?.let { Uri.parse(it) }

                    val note = NoteItem(
                        content = content,
                        isChecklist = true,
                        checklistItems = checklistItems,
                        isImage = imageUri != null,
                        isVideo = videoUri != null,
                        isAudio = audioUri != null,
                        isFile = fileUri != null,
                        uri = imageUri ?: videoUri ?: audioUri ?: fileUri
                    )
                    notesList.add(note)
                }
                notesAdapter.notifyDataSetChanged()
                tvNoNotes.visibility = if (notesList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
}
