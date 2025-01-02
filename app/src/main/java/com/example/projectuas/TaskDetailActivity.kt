package com.example.projectuas

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var notesAdapter: NotesAdapter
    private val notesList = mutableListOf<NoteItem>()
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvTaskTitle: TextView
    private lateinit var tvNoNotes: TextView
    private lateinit var taskName: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Inisialisasi views
        tvTaskTitle = findViewById(R.id.tvTaskTitle)
        tvNoNotes = findViewById(R.id.tvNoNotes) // Inisialisasi tvNoNotes
        notesAdapter = NotesAdapter(notesList) { position, note ->
            deleteNote(position, note)
        }
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

    private fun deleteNote(position: Int, note: NoteItem) {
        val projectId = intent.getStringExtra("projectId") ?: return

        // Tampilkan loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Menghapus note...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        val noteRef = firestore.collection("projects")
            .document(projectId)
            .collection("taskList")
            .document(taskName)
            .collection("notes")
            .document(note.documentId)

        // Buat list untuk menyimpan semua promise penghapusan
        val deleteTasks = mutableListOf<Task<Void>>()

        // Tambahkan task penghapusan file dari Storage jika ada
        if (note.isImage || note.isVideo || note.isAudio || note.isFile) {
            note.uri?.let { uri ->
                try {
                    val fileRef = storage.getReferenceFromUrl(uri.toString())
                    deleteTasks.add(fileRef.delete())
                } catch (e: Exception) {
                    // Handle jika URL tidak valid
                    Log.e("DeleteNote", "Error getting storage reference: ${e.message}")
                }
            }
        }

        // Tambahkan task penghapusan thumbnail video jika ada
        if (note.isVideo && !note.videoThumbnailUri.isNullOrEmpty()) {
            try {
                val thumbnailRef = storage.getReferenceFromUrl(note.videoThumbnailUri)
                deleteTasks.add(thumbnailRef.delete())
            } catch (e: Exception) {
                Log.e("DeleteNote", "Error getting thumbnail reference: ${e.message}")
            }
        }

        // Tambahkan task penghapusan dokumen Firestore
        deleteTasks.add(noteRef.delete())

        // Jalankan semua task penghapusan secara bersamaan
        Tasks.whenAll(deleteTasks)
            .addOnSuccessListener {
                // Semua penghapusan berhasil
                loadingDialog.dismiss()
                notesList.removeAt(position)
                notesAdapter.notifyItemRemoved(position)
                Toast.makeText(this, "Note berhasil dihapus", Toast.LENGTH_SHORT).show()
                tvNoNotes.visibility = if (notesList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                // Ada kesalahan dalam penghapusan
                loadingDialog.dismiss()
                Toast.makeText(this, "Gagal menghapus note: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                    val audioUri = noteDoc.getString("audioUrl")?.let { Uri.parse(it) }
                    val fileUri = noteDoc.getString("fileUri")?.let { Uri.parse(it) }
                    val fileName = noteDoc.getString("fileName")
                    val videoThumbnailUri = noteDoc.getString("videoThumbnailUri")

                    val note = NoteItem(
                        documentId = noteDoc.id,  // Tambahkan documentId
                        content = content,
                        isChecklist = checklistItems.isNotEmpty(),
                        checklistItems = checklistItems,
                        isImage = imageUri != null,
                        isVideo = videoUri != null,
                        isAudio = audioUri != null,
                        isFile = fileUri != null,
                        fileName = fileName,
                        uri = if (videoUri != null) videoUri else imageUri ?: audioUri ?: fileUri,
                        audioUrl = audioUri,
                        videoThumbnailUri = videoThumbnailUri
                    )
                    notesList.add(note)
                }
                notesAdapter.notifyDataSetChanged()
                tvNoNotes.visibility = if (notesList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil notes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
