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
    PrivateTaskAdapter.SendToArchiveClickListener,
    ProjectTaskAdapter.OnProjectDoneClickListener {

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

    override fun onResume() {
        super.onResume()
        fetchProjects()
    }

    private fun fetchProjects() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val currentUserName = sharedPref.getString("username", "")

        // Query untuk projects dimana user adalah admin atau member
        firestore.collection("projects")
            .whereEqualTo("isArchived", false) // Only non-archived projects
            .get()
            .addOnSuccessListener { documents ->
                projectTasks.clear()
                privateTasksList.clear()

                for (document in documents) {
                    // Check if user is admin
                    val isAdmin = document.getString("userId") == currentUser.uid
                    // Check if user is member
                    val memberList = document.get("memberList") as? List<String> ?: listOf()
                    val isMember = memberList.contains("${currentUser.email} ($currentUserName)")

                    if (isAdmin || isMember) {
                        addProjectToList(document)
                    }
                }
                updateRecyclerViews()
            }
    }

    private fun addProjectToList(document: DocumentSnapshot) {
        val projectName = document.getString("projectTitle") ?: "No Title"
        val teamMembers = document.get("memberList") as? List<String> ?: listOf()
        val progress = "On Progress"
        val projectImage1 = R.drawable.img_4
        val projectId = document.id

        if (teamMembers.isNotEmpty()) {
            projectTasks.add(
                ProjectTask(
                    projectId = projectId,
                    projectName = projectName,
                    teamMembers = teamMembers,
                    progress = progress,
                    projectImage1 = projectImage1
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
        // Siapkan Adapter dan LayoutManager untuk Project Tasks (Horizontal)
        projectTaskAdapter = ProjectTaskAdapter(
            projectTasks,
            doneListener = this, // Untuk mendeteksi tombol "done"
            archiveListener = object : ProjectTaskAdapter.OnProjectArchiveClickListener {
                override fun onProjectArchiveClick(position: Int) {
                    val projectTask = projectTasks[position]
                    archiveProjectTask(projectTask, position) // Panggil logika archive
                }
            }
        )
        rvProjectTasks.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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

        projectTaskAdapter.setOnProjectClickListener(object :
            ProjectTaskAdapter.OnProjectClickListener {
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
                Toast.makeText(
                    requireContext(),
                    "Error loading project: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error fetching projects: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Implementasi Interface OnDeleteClickListener (PrivateTaskAdapter)
    override fun sendToArchiveClick(position: Int) {
        val privateTask = privateTasksList[position]
        archivePrivateTask(privateTask, position)
    }

    // Implementasi Interface OnProjectDeleteClickListener (ProjectTaskAdapter)
    override fun onProjectDoneClick(position: Int) {
        val projectTask = projectTasks[position]
        deleteProjectTask(projectTask.projectId, position)
    }

    private fun archivePrivateTask(privateTask: PrivateTask, position: Int) {

        val currentUser = auth.currentUser

        firestore.collection("projects").document(privateTask.projectId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val archiveTask = mapOf(
                        "projectId" to privateTask.projectId,
                        "taskName" to privateTask.taskName,
                        "progress" to privateTask.progress,
                        "projectDetail" to (document.getString("projectDetail") ?: ""),
                        "dueDate" to (document.getString("dueDate") ?: ""),
                        "taskList" to (document.get("taskList") as? List<String> ?: listOf()),
                        "memberList" to (document.get("memberList") as? List<String> ?: listOf()),
                        "userId" to currentUser?.uid, // Add current user ID
                        "isArchived" to true

                    )

                    firestore.collection("archive")
                        .add(archiveTask)
                        .addOnSuccessListener {
                            // Hapus dokumen dari koleksi "projects"
                            firestore.collection("projects").document(privateTask.projectId)
                                .update("isArchived", true)
                                .addOnSuccessListener {
                                    // Hapus dari list lokal dan beri tahu adapter
                                    privateTasksList.removeAt(position)
                                    privateTaskAdapter.notifyItemRemoved(position)
                                    Toast.makeText(
                                        requireContext(),
                                        "Private Task archived successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to delete task from projects: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Failed to archive task: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Project not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch project: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun archiveProjectTask(projectTask: ProjectTask, position: Int) {
        firestore.collection("projects").document(projectTask.projectId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Data untuk koleksi Archive
                    val archiveTask = mapOf(
                        "projectId" to projectTask.projectId,
                        "taskName" to projectTask.projectName, // Menyesuaikan ke taskName
                        "progress" to projectTask.progress, // Tambahkan progress
                        "projectDetail" to (document.getString("projectDetail") ?: ""),
                        "dueDate" to (document.getString("dueDate") ?: ""),
                        "taskList" to (document.get("taskList") as? List<String> ?: listOf()),
                        "memberList" to (document.get("memberList") as? List<String> ?: listOf()),
                        "userId" to (document.getString("userId") ?: ""),
                        "adminId" to (document.getString("adminId") ?: ""),
                        "roles" to (document.get("roles") ?: mapOf<String, String>())
                    )

                    // Tambahkan data ke koleksi Archive
                    firestore.collection("archive")
                        .add(archiveTask)
                        .addOnSuccessListener {
                            // Perbarui dokumen di koleksi Projects dengan isArchived = true
                            firestore.collection("projects").document(projectTask.projectId)
                                .update("isArchived", true)
                                .addOnSuccessListener {
                                    // Hapus dari daftar lokal dan beri tahu adapter
                                    projectTasks.removeAt(position)
                                    projectTaskAdapter.notifyItemRemoved(position)
                                    Toast.makeText(
                                        requireContext(),
                                        "Project Task archived successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to update project status: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Failed to archive task: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Project not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch project: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun deleteProjectTask(projectId: String, position: Int) {
        firestore.collection("projects").document(projectId)
            .delete()
            .addOnSuccessListener {
                // Hapus dari list dan notifikasi adapter
                projectTasks.removeAt(position)
                projectTaskAdapter.notifyItemRemoved(position)
                Toast.makeText(requireContext(), "Project deleted successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to delete project: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}