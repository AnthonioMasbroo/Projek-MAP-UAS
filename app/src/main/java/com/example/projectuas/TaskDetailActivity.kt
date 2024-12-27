package com.example.projectuas

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Pengguna tidak terautentikasi", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("projects")
            .document(projectId)
            .collection("taskList")
            .document(taskName)
            .collection("notes")
            .get()
            .addOnSuccessListener { notesSnapshot ->
                val taskNotes = mutableListOf<NoteItem>()

                for (noteDoc in notesSnapshot) {
                    val noteType = noteDoc.getString("type") ?: "text"
                    val content = noteDoc.getString("content") ?: ""
                    val uriString = noteDoc.getString("uri")
                    val uri = uriString?.let { Uri.parse(it) }

                    val note = when (noteType) {
                        "text" -> NoteItem(content = content)
                        "image" -> NoteItem(content = "Image Note", isImage = true, uri = uri)
                        "video" -> NoteItem(content = "Video Note", isVideo = true, uri = uri)
                        "audio" -> NoteItem(content = "Audio Note", isAudio = true, uri = uri)
                        "file" -> NoteItem(content = "File Note", isFile = true, uri = uri)
                        "checklist" -> {
                            val checklistItems = (noteDoc.get("checklistItems") as? List<Map<String, Any>>)
                                ?.map { checklistMap ->
                                    ChecklistItem(
                                        description = checklistMap["description"] as? String ?: "",
                                        isChecked = checklistMap["isChecked"] as? Boolean ?: false
                                    )
                                } ?: listOf()
                            NoteItem(content = "Checklist Note", isChecklist = true, checklistItems = checklistItems)
                        }
                        else -> NoteItem(content = content)
                    }

                    taskNotes.add(note)
                }

                // Perbarui RecyclerView dengan catatan yang ditemukan
                runOnUiThread {
                    notesList.clear()
                    notesList.addAll(taskNotes)
                    notesAdapter.notifyDataSetChanged()

                    // Tampilkan atau sembunyikan placeholder berdasarkan jumlah catatan
                    if (notesList.isEmpty()) {
                        tvNoNotes.visibility = View.VISIBLE
                    } else {
                        tvNoNotes.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TaskDetailActivity", "Error fetching notes", e) // Logging untuk debugging
                Toast.makeText(this, "Gagal mengambil catatan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
