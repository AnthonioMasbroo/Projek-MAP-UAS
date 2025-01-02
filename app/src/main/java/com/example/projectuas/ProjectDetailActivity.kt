package com.example.projectuas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.projectuas.models.Project
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.annotation.SuppressLint
import android.widget.Toast
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog


class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var project: Project
    private lateinit var backButton: ImageView
    private lateinit var btnEdit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        backButton = findViewById(R.id.backButton)
        btnEdit = findViewById(R.id.btnEdit)

        // Ensure projectData is present and has documentId
        project = intent.getParcelableExtra<Project>("projectData") ?: run {
            Toast.makeText(this, "Project data missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("ProjectDetailActivity", "Project Data: $project")

        if (project.documentId.isEmpty()) {
            Toast.makeText(this, "Invalid project data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Pindahkan checkUserRole() setelah project diinisialisasi
        checkUserRole()
        // Disable edit button if the project is archived
        val isArchived = intent.getBooleanExtra("isArchived", false)
        if (isArchived) {
            btnEdit.isEnabled = false // Disable button
            btnEdit.alpha = 0.5f      // Optional: Reduce opacity to indicate disabled state
        }

        displayProjectDetails()

        // Back Button Logic
        backButton.setOnClickListener {
            navigateToHome()
        }

        // Edit Button Logic
        btnEdit.setOnClickListener {
            val intent = Intent(this, AddProjectActivity::class.java).apply {
                putExtra("projectData", project)
                putExtra("isEditMode", true)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.apply {
            putExtra("MODE", "HOME")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    // Override onBackPressed untuk menangani tombol back hardware
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        navigateToHome()
    }

    private fun checkUserRole() {
        firestore.collection("projects").document(project.documentId).get()
            .addOnSuccessListener { doc ->
                val adminId = doc.getString("adminId")
                val currentUserId = auth.currentUser?.uid


                // Cek juga roles map
                val roles = doc.get("roles") as? Map<String, String>
                val userRole = roles?.get(currentUserId)


                btnEdit.visibility = if (adminId == currentUserId || userRole == "admin") View.VISIBLE else View.GONE
            }
    }


    @SuppressLint("NewApi")
    private fun displayProjectDetails() {
        // Set project title, detail, and due date
        findViewById<TextView>(R.id.tvProjectTitle).text = project.projectTitle
        findViewById<TextView>(R.id.projectDetailContent).text = project.projectDetail
        findViewById<TextView>(R.id.dueDateContent).text = project.dueDate

        val llTeamMember = findViewById<TextView>(R.id.llTeamMember)
        // Update Team Members count and set click listener for member details
        val memberCount = project.memberList.size
        llTeamMember.text = if (memberCount > 0) {
            "$memberCount"
        } else {
            "No team members invited"
        }
        findViewById<ImageView>(R.id.ivMembers).setOnClickListener {
            showMemberListDialog()
        }



        // Setup task list
        val llTaskList = findViewById<LinearLayout>(R.id.llTaskList)
        llTaskList.removeAllViews()

        project.taskList.forEach { task ->
            val taskCard = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 0)
                }
                setBackgroundResource(R.drawable.input_date)
                setPadding(50, 50, 50, 50)
                setOnClickListener {
                    startActivity(Intent(this@ProjectDetailActivity, TaskDetailActivity::class.java).apply {
                        putExtra("taskName", task)
                        putExtra("projectId", project.documentId)
                    })
                }

                addView(TextView(context).apply {
                    text = task
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.bg, null))
                    typeface = resources.getFont(R.font.poppinsmedium)
                    setPadding(0, 10, 0, 10)
                })
            }
            llTaskList.addView(taskCard)
        }
    }

    private fun showMemberListDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Team Members")
            .setView(layoutInflater.inflate(R.layout.dialog_member_list, null).apply {
                val container = findViewById<LinearLayout>(R.id.memberListContainer)

                // Tampilkan Creator/Admin
                firestore.collection("projects").document(project.documentId).get()
                    .addOnSuccessListener { doc ->
                        val adminId = doc.getString("adminId")
                        firestore.collection("users").document(adminId ?: "").get()
                            .addOnSuccessListener { adminDoc ->
                                val adminName = adminDoc.getString("username")
                                val adminEmail = adminDoc.getString("email")

                                container.addView(TextView(context).apply {
                                    text = "Creator/Admin:\nName: $adminName\nEmail: $adminEmail"
                                    textSize = 16f
                                    setTextColor(resources.getColor(R.color.tv_color, null))
                                    typeface = resources.getFont(R.font.poppinsregular)
                                    setPadding(20, 20, 20, 20)
                                    setBackgroundResource(R.drawable.input_shape)
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        setMargins(0, 10, 0, 10)
                                    }
                                })
                            }
                    }

                // Tampilkan anggota tim
                project.memberList.forEach { member ->
                    val memberParts = member.split(" (")
                    val email = memberParts[0]
                    val username = memberParts[1].removeSuffix(")")

                    container.addView(TextView(context).apply {
                        text = "Team Member:\nName: $username\nEmail: $email"
                        textSize = 16f
                        setTextColor(resources.getColor(R.color.tv_color, null))
                        typeface = resources.getFont(R.font.poppinsregular)
                        setPadding(20, 20, 20, 20)
                        setBackgroundResource(R.drawable.input_shape)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 10, 0, 10)
                        }
                    })
                }
            })
            .setPositiveButton("Close", null)
            .create()

        dialog.show()
    }
}