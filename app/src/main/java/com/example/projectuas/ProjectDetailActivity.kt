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

        project = intent.getParcelableExtra<Project>("projectData") ?: Project("", "", "", listOf(), listOf(), "")

        displayProjectDetails()

        // Back Button Logic dengan implementasi baru
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Edit Button Logic
        btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("projectData", project)
            }

            val addProjectFragment = AddProjectFragment().apply {
                arguments = bundle
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("openFragment", "add_project")
                putExtra("projectData", project)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun returnToHome() {
        // Membuat intent baru untuk MainActivity
        val intent = Intent(this, MainActivity::class.java)
        // Membersihkan semua activity yang ada di stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        // Menambahkan flag khusus untuk HomeFragment
        intent.putExtra("RETURN_TO_HOME", true)
        startActivity(intent)
        // Menutup activity saat ini
        finish()
    }

    // Override onBackPressed untuk menangani tombol back hardware
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("RETURN_TO_HOME", true)
        }
        startActivity(intent)
        finish()
    }




    @SuppressLint("NewApi")
    private fun displayProjectDetails() {
        // Kode displayProjectDetails tetap sama
        findViewById<TextView>(R.id.tvProjectTitle).text = project.projectTitle
        findViewById<TextView>(R.id.projectDetailContent).text = project.projectDetail
        findViewById<TextView>(R.id.dueDateContent).text = project.dueDate

        val llTeamMember = findViewById<LinearLayout>(R.id.llTeamMember)
        llTeamMember.removeAllViews()

        if (project.memberList.isEmpty()) {
            val noMemberTextView = TextView(this).apply {
                text = "No team members"
                textSize = 16f
                setTextColor(resources.getColor(R.color.tv_color, null))
                typeface = resources.getFont(R.font.poppinsregular)
                setPadding(0, 10, 0, 10)
            }
            llTeamMember.addView(noMemberTextView)
        } else {
            project.memberList.forEach { member ->
                val memberTextView = TextView(this).apply {
                    text = member.split(" (")[0]
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.tv_color, null))
                    typeface = resources.getFont(R.font.poppinsregular)
                    setPadding(0, 10, 0, 10)
                }
                llTeamMember.addView(memberTextView)
            }
        }

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
                    val intent = Intent(this@ProjectDetailActivity, TaskDetailActivity::class.java).apply {
                        putExtra("taskData", task)
                    }
                    startActivity(intent)
                }
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
    }
}