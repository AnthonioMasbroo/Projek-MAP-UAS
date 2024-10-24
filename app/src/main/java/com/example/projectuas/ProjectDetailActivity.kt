package com.example.projectuas

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var projectId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get the projectId from the Intent
        projectId = intent.getStringExtra("projectId") ?: ""

        // Load project details
        loadProjectDetails()
    }

    private fun loadProjectDetails() {
        // Reference to the project document in Firestore
        firestore.collection("projects").document(projectId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Retrieve project details
                    val projectTitle = document.getString("projectTitle")
                    val projectDetail = document.getString("projectDetail")
                    val dueDate = document.getString("dueDate")
                    val memberList = document.get("memberList") as? List<String> // Dapatkan memberList sebagai List
                    val taskList = document.get("taskList") as? List<String>

                    // Update UI elements
                    findViewById<TextView>(R.id.tvProjectTitle).text = projectTitle
                    findViewById<TextView>(R.id.tvProjectDetail).text = projectDetail
                    findViewById<TextView>(R.id.tvDueDate).text = dueDate

                    // Handle team member list
                    val llTeamMember = findViewById<LinearLayout>(R.id.llTeamMember)
                    llTeamMember.removeAllViews() // Clear existing views
                    memberList?.forEach { member ->
                        val memberTextView = TextView(this).apply {
                            text = member
                            textSize = 16f
                            setPadding(0, 10, 0, 10)
                        }
                        llTeamMember.addView(memberTextView) // Tambahkan setiap anggota tim ke UI
                    }

                    // Handle task list
                    val llTaskList = findViewById<LinearLayout>(R.id.llTaskList)
                    llTaskList.removeAllViews() // Clear existing views
                    taskList?.forEach { task ->
                        val taskView = layoutInflater.inflate(R.layout.item_task, null)
                        val taskText = taskView.findViewById<TextView>(R.id.taskName)
                        val taskIcon = taskView.findViewById<ImageView>(R.id.taskIcon)
                        taskText.text = task
                        taskIcon.setImageResource(R.drawable.img)
                        llTaskList.addView(taskView) // Tambahkan setiap task ke UI
                    }
                } else {
                    Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load project.", Toast.LENGTH_SHORT).show()
            }
    }
}

