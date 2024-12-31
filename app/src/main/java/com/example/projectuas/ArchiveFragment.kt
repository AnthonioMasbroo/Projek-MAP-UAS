package com.example.projectuas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectuas.adapters.ArchiveAdapter
import com.example.projectuas.models.PrivateTask
import com.example.projectuas.models.Project
import com.google.firebase.firestore.FirebaseFirestore

class ArchiveFragment : Fragment(), ArchiveAdapter.OnDeleteClickListener, ArchiveAdapter.OnRestoreClickListener {

    private lateinit var rvArchivedTasks: RecyclerView
    private lateinit var archiveAdapter: ArchiveAdapter
    private lateinit var firestore: FirebaseFirestore

    private val archivedTasksList = mutableListOf<PrivateTask>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_archive, container, false)

        rvArchivedTasks = view.findViewById(R.id.rvArchivedTasks)
        firestore = FirebaseFirestore.getInstance()

        // Fetch archived tasks
        fetchArchivedTasks()

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchArchivedTasks()
    }

    private fun fetchArchivedTasks() {
        firestore.collection("archive")
            .get()
            .addOnSuccessListener { documents ->
                archivedTasksList.clear()
                for (document in documents) {
                    val projectId = document.getString("projectId") ?: ""
                    val taskName = document.getString("taskName") ?: "No Task Name"
                    val progress = document.getString("progress") ?: "Unknown"

                    archivedTasksList.add(
                        PrivateTask(
                            projectId = document.id, // Gunakan document.id untuk referensi dokumen
                            taskName = taskName,
                            progress = progress
                        )
                    )
                }

                // Setup Adapter and LayoutManager
                archiveAdapter = ArchiveAdapter(archivedTasksList, this,this )
                rvArchivedTasks.layoutManager = LinearLayoutManager(context)
                rvArchivedTasks.adapter = archiveAdapter

                archiveAdapter.setOnItemClickListener(object : ArchiveAdapter.OnItemClickListener {
                    override fun onItemClick(archiveId: String, taskName: String) {
                        // Fetch full project data dari Firestore
                        firestore.collection("archive").document(archiveId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    // Convert document ke Project object
                                    val project = Project(
                                        documentId = document.getString("projectId") ?: "",
                                        projectTitle = document.getString("taskName") ?: "",
                                        projectDetail = document.getString("projectDetail") ?: "",
                                        dueDate = document.getString("dueDate") ?: "",
                                        taskList = (document.get("taskList") as? List<String>) ?: listOf(),
                                        memberList = (document.get("memberList") as? List<String>) ?: listOf(),
                                        userId = document.getString("userId") ?: ""
                                    )

                                    // Navigate ke ProjectDetailActivity
                                    val intent = Intent(requireContext(), ProjectDetailActivity::class.java).apply {
                                        putExtra("projectData", project)
                                        putExtra("isArchived", true) // Add this flag
                                    }
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(requireContext(), "Project not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error loading project: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                })
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch archived tasks: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Implementasi Interface OnDeleteClickListener (ArchiveAdapter)
    override fun onDeleteClick(archiveId: String, position: Int) {
        firestore.collection("archive").document(archiveId)
            .delete()
            .addOnSuccessListener {
                // Hapus dari list dan notifikasi adapter
                archivedTasksList.removeAt(position)
                archiveAdapter.notifyItemRemoved(position)
                Toast.makeText(requireContext(), "Private Task deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Implementasi Interface OnRestoreClickListener (ArchiveAdapter)
    override fun onRestoreClick(archiveId: String, position: Int) {
        // Ambil data dari archive
        firestore.collection("archive").document(archiveId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val restoredTask = mapOf(
                        "projectTitle" to document.getString("taskName"),
                        "progress" to document.getString("progress"),
                        "isArchived" to false, // Set isArchived menjadi false
                        "taskList" to document.get("taskList"),
                        "memberList" to document.get("memberList"),
                        "userId" to document.getString("userId"),
                        "dueDate" to document.getString("dueDate"),
                        "projectDetail" to document.getString("projectDetail")
                    )

                    // Tambahkan kembali ke koleksi "projects"
                    firestore.collection("projects")
                        .add(restoredTask)
                        .addOnSuccessListener {
                            // Hapus dari archive
                            firestore.collection("archive").document(archiveId)
                                .delete()
                                .addOnSuccessListener {
                                    archivedTasksList.removeAt(position)
                                    archiveAdapter.notifyItemRemoved(position)
                                    Toast.makeText(requireContext(), "Task restored successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Failed to delete from archive: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to restore task: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Task not found in archive", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}