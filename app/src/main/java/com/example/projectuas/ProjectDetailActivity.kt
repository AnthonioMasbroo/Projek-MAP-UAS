package com.example.projectuas

import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize backButton
        backButton = findViewById(R.id.backButton)

        // Get the projectId from the Intent
        projectId = intent.getStringExtra("projectId") ?: ""

        // Log the projectId to verify if it's correct
        Log.d("ProjectDetailActivity", "Project ID: $projectId")

        // Load project details
        loadProjectDetails()

        // Back Button Logic
        backButton.setOnClickListener {
            finish()  // Tutup aktivitas saat ini dan kembali ke MainActivity
        }

    }

    @SuppressLint("NewApi")
    private fun loadProjectDetails() {
        // Reference to the project document in Firestore
        firestore.collection("projects").document(projectId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Retrieve project details
                    val projectTitle = document.getString("projectTitle")
                    val projectDetail = document.getString("projectDetail")
                    val dueDate = document.getString("dueDate")
                    val memberList = document.get("memberList") as? List<String> // Dapatkan memberList
                    val taskList = document.get("taskList") as? List<String> // Dapatkan taskList

                    // Update UI elements
                    findViewById<TextView>(R.id.tvProjectTitle).text = projectTitle
                    findViewById<TextView>(R.id.projectDetailContent).text = projectDetail
                    findViewById<TextView>(R.id.dueDateContent).text = dueDate

                    // Display team members
                    val llTeamMember = findViewById<LinearLayout>(R.id.llTeamMember)
                    llTeamMember.removeAllViews() // Clear existing views
                    memberList?.forEach { member ->
                        val memberTextView = TextView(this).apply {
                            text = member
                            textSize = 16f
                            setPadding(0, 10, 0, 10)
                        }
                        llTeamMember.addView(memberTextView)
                    }

                    // Display task list
                    val llTaskList = findViewById<LinearLayout>(R.id.llTaskList)
                    llTaskList.removeAllViews() // Clear existing views
                    taskList?.forEach { task ->
                        val taskCard = LinearLayout(this).apply{
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                    setMargins(0, 20, 0, 0)
                            }
                            setBackgroundResource(R.drawable.input_date)
                            setPadding(50, 50, 50, 50)
                        }

                        val taskTextView = TextView(this).apply {
                            text = task
                            textSize = 16f
                            setTextColor(resources.getColor(R.color.bg, null))
                            typeface = resources.getFont(R.font.poppinsmedium)
                            setPadding(0, 10, 0, 10)
                        }
                        taskCard.addView(taskTextView)
                        llTaskList.addView(taskCard)
                    }
                } else {
                    Log.d("ProjectDetailActivity", "Document not found for project ID: $projectId")
                    Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProjectDetailActivity", "Error loading project: ${exception.message}")
                Toast.makeText(this, "Failed to load project: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}


