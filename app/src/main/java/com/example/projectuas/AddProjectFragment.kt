package com.example.projectuas

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddProjectFragment : Fragment(R.layout.fragment_add_project) {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var taskList: MutableList<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        taskList = mutableListOf()

        val addTaskButton: LinearLayout = view.findViewById(R.id.addTaskButton)
        val btnAddProject: Button = view.findViewById(R.id.btnAddProject)
        val ivDateTime: ImageView = view.findViewById(R.id.ivDateTime)
        val etDateTime: EditText = view.findViewById(R.id.etDateTime)

        // Handle add task button
        addTaskButton.setOnClickListener {
            addTaskField()
        }

        // Handle create project button
        btnAddProject.setOnClickListener {
            createProject()
        }

        // Handle calendar icon click
        ivDateTime.setOnClickListener {
            showDatePickerDialog(etDateTime)
        }
    }

    @SuppressLint("NewApi")
    private fun addTaskField() {
        val linearLayoutContainer: LinearLayout? = view?.findViewById(R.id.linearLayoutContainer)
        // Dynamically add a new EditText for a task
        val newTaskEditText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0) // Set top margin to 15dp
            }
            hint = "Task"
            setBackgroundResource(R.drawable.input_date) // Set the background drawable
            setPadding(50, 50, 20, 50) // Set padding to make height larger than content
            setTextColor(resources.getColor(R.color.bg, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            setTextSize(20f)
        }
        linearLayoutContainer?.addView(newTaskEditText)
    }

    private fun createProject() {
        val projectTitle = view?.findViewById<EditText>(R.id.etProjectTitle)?.text.toString().trim()
        val projectDetail = view?.findViewById<EditText>(R.id.etProjectDetail)?.text.toString().trim()
        val dueDate = view?.findViewById<EditText>(R.id.etDateTime)?.text.toString().trim()
        val teamMember = view?.findViewById<TextView>(R.id.AddTeamMember)?.text.toString().trim()
        val linearLayoutContainer: LinearLayout? = view?.findViewById(R.id.linearLayoutContainer)

        // Validate inputs
        if (projectTitle.isEmpty() || projectDetail.isEmpty() || dueDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear taskList first, and retrieve all task input values from the EditTexts
        taskList.clear()
        linearLayoutContainer?.let {
            for (i in 0 until it.childCount) {
                val taskEditText = it.getChildAt(i) as? EditText
                taskEditText?.text?.toString()?.let { task ->
                    if (task.isNotEmpty()) {
                        taskList.add(task)
                    }
                }
            }
        }

        // Check if current user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare data to be saved
        val projectData = hashMapOf(
            "projectTitle" to projectTitle,
            "projectDetail" to projectDetail,
            "teamMember" to teamMember,
            "dueDate" to dueDate,
            "taskList" to taskList,
            "userId" to currentUser.uid
        )

        // Save project data to Firestore
        firestore.collection("projects")
            .add(projectData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(requireContext(), "Project added successfully", Toast.LENGTH_SHORT).show()
                // Navigate to ProjectDetailActivity
                val intent = Intent(requireContext(), ProjectDetailActivity::class.java).apply {
                    putExtra("projectId", documentReference.id)
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to add project: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            editText.setText(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }
}