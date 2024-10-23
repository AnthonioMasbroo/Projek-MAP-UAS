package com.example.projectuas

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)

        val projectId = intent.getStringExtra("projectId")

        firestore = FirebaseFirestore.getInstance()

        // Get project data from Firestore
        projectId?.let {
            firestore.collection("projects").document(it)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        findViewById<TextView>(R.id.tvProjectTitle).text = document.getString("projectTitle")
                        findViewById<TextView>(R.id.tvProjectDetail).text = document.getString("projectDetail")
                        findViewById<TextView>(R.id.tvTimeDate).text = document.getString("dueDate")
                        findViewById<TextView>(R.id.tvTeamMember).text = document.getString("teamMember")

                        // Display task list
                        val taskList = document.get("taskList") as? List<String>
                        taskList?.forEach { task ->
                            val textView = TextView(this)
                            textView.text = task
                            findViewById<LinearLayout>(R.id.linearLayoutTasks).addView(textView)
                        }
                    }
                }
        }
    }
}
