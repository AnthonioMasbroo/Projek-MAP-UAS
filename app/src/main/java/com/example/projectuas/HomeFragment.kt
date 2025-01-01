package com.example.projectuas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectuas.adapters.PrivateTaskAdapter
import com.example.projectuas.adapters.ProjectTaskAdapter
import com.example.projectuas.models.PrivateTask
import com.example.projectuas.models.ProjectTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.projectuas.models.Project
import com.google.firebase.firestore.DocumentSnapshot

class HomeFragment : Fragment(),
    PrivateTaskAdapter.OnDeleteClickListener,
    ProjectTaskAdapter.OnProjectDeleteClickListener {

    private lateinit var rvProjectTasks: RecyclerView
    private lateinit var rvPrivateTasks: RecyclerView
    private lateinit var projectTaskAdapter: ProjectTaskAdapter
    private lateinit var privateTaskAdapter: PrivateTaskAdapter

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val projectTasks = mutableListOf<ProjectTask>()
    private val privateTasksList = mutableListOf<PrivateTask>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val tvWelcome: TextView = view.findViewById(R.id.tvWelcome)

        // Ambil username dari SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") // Default jika username tidak ada

        // Tampilkan username di Welcome message
        tvWelcome.text = "Welcome, $username"

        // Inisialisasi RecyclerViews
        rvProjectTasks = view.findViewById(R.id.rvProjectTasks)
        rvPrivateTasks = view.findViewById(R.id.rvPrivateTasks)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Fetch projects from Firestore
        fetchProjects()

        return view
    }

    private fun fetchProjects() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val currentUserName = sharedPref.getString("username", "")

        // Query untuk mendapatkan semua projects
        firestore.collection("projects")
            .whereEqualTo("userId", currentUser.uid) // Projects dimana user adalah admin
            .get()
            .addOnSuccessListener { adminDocs ->
                projectTasks.clear()
                privateTasksList.clear()

                // Tambahkan projects dimana user adalah admin
                for (document in adminDocs) {
                    addProjectToList(document)
                }

                // Query kedua untuk projects dimana user adalah member
                firestore.collection("projects")
                    .whereArrayContains("memberList", "${currentUser.email} ($currentUserName)")
                    .get()
                    .addOnSuccessListener { memberDocs ->
                        // Tambahkan projects dimana user adalah member
                        for (document in memberDocs) {
                            // Skip jika project sudah ada (untuk menghindari duplikat)
                            if (!projectTasks.any { it.projectId == document.id }) {
                                addProjectToList(document)
                            }
                        }

                        // Update RecyclerViews
                        updateRecyclerViews()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching projects: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addProjectToList(document: DocumentSnapshot) {
        val projectName = document.getString("projectTitle") ?: "No Title"
        val teamMembers = document.get("memberList") as? List<String> ?: listOf()
        val progress = "On Progress"
        val projectImage1 = R.drawable.user_8109090
        val projectImage2 = R.drawable.user_10923836
        val projectId = document.id

        if (teamMembers.isNotEmpty()) {
            projectTasks.add(
                ProjectTask(
                    projectId = projectId,
                    projectName = projectName,
                    teamMembers = teamMembers,
                    progress = progress,
                    projectImage1 = projectImage1,
                    projectImage2 = projectImage2
                )
            )
        } else {
            privateTasksList.add(
                PrivateTask(
                    projectId = projectId,
                    taskName = projectName,
                    progress = progress
                )
            )
        }
    }

    private fun updateRecyclerViews() {
        // Setup ProjectTask Adapter
        projectTaskAdapter = ProjectTaskAdapter(projectTasks, this)
        rvProjectTasks.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvProjectTasks.adapter = projectTaskAdapter

        // Setup PrivateTask Adapter
        privateTaskAdapter = PrivateTaskAdapter(privateTasksList, this)
        rvPrivateTasks.layoutManager = LinearLayoutManager(context)
        rvPrivateTasks.adapter = privateTaskAdapter

        // Setup click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        privateTaskAdapter.setOnItemClickListener(object : PrivateTaskAdapter.OnItemClickListener {
            override fun onItemClick(projectId: String, taskName: String) {
                fetchAndNavigateToProjectDetail(projectId)
            }
        })

        projectTaskAdapter.setOnProjectClickListener(object : ProjectTaskAdapter.OnProjectClickListener {
            override fun onProjectClick(projectId: String) {
                fetchAndNavigateToProjectDetail(projectId)
            }
        })
    }

    private fun fetchAndNavigateToProjectDetail(projectId: String) {
        firestore.collection("projects").document(projectId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val project = Project(
                        documentId = document.id,
                        projectTitle = document.getString("projectTitle") ?: "",
                        projectDetail = document.getString("projectDetail") ?: "",
                        dueDate = document.getString("dueDate") ?: "",
                        taskList = (document.get("taskList") as? List<String>) ?: listOf(),
                        memberList = (document.get("memberList") as? List<String>) ?: listOf(),
                        userId = document.getString("userId") ?: ""
                    )

                    val intent = Intent(requireContext(), ProjectDetailActivity::class.java)
                    intent.putExtra("projectData", project)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Project not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading project: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Implementasi Interface OnDeleteClickListener (PrivateTaskAdapter)
    override fun onDeleteClick(position: Int) {
        val privateTask = privateTasksList[position]
        deletePrivateTask(privateTask.projectId, position)
    }

    // Implementasi Interface OnProjectDeleteClickListener (ProjectTaskAdapter)
    override fun onProjectDeleteClick(position: Int) {
        val projectTask = projectTasks[position]
        deleteProjectTask(projectTask.projectId, position)
    }

    private fun deletePrivateTask(projectId: String, position: Int) {
        firestore.collection("projects").document(projectId)
            .delete()
            .addOnSuccessListener {
                // Hapus dari list dan notifikasi adapter
                privateTasksList.removeAt(position)
                privateTaskAdapter.notifyItemRemoved(position)
                Toast.makeText(requireContext(), "Private Task deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteProjectTask(projectId: String, position: Int) {
        firestore.collection("projects").document(projectId)
            .delete()
            .addOnSuccessListener {
                // Hapus dari list dan notifikasi adapter
                projectTasks.removeAt(position)
                projectTaskAdapter.notifyItemRemoved(position)
                Toast.makeText(requireContext(), "Project deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete project: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
