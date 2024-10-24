package com.example.projectuas

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
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
    private lateinit var memberList: MutableList<String> // List untuk member

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        taskList = mutableListOf()
        memberList = mutableListOf() // Inisialisasi member list

        val addTaskButton: LinearLayout = view.findViewById(R.id.addTaskButton)
        val btnAddProject: Button = view.findViewById(R.id.btnAddProject)
        val ivDateTime: ImageView = view.findViewById(R.id.ivDateTime)
        val etDateTime: EditText = view.findViewById(R.id.etDateTime)
        val ivAddTeamMember: ImageView = view.findViewById(R.id.ivAddTeamMember) // Icon untuk tambah member

        // Handle add task button
        addTaskButton.setOnClickListener {
            addTaskField()
        }

        // Handle add member button (icon)
        ivAddTeamMember.setOnClickListener {
            addMemberField()
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


    @SuppressLint("NewApi")
    private fun addMemberField() {
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)

        // Buat LinearLayout Horizontal untuk Email dan Username
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0) // Margin top 20dp
            }
        }

        // Card untuk Email
        val emailCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0, // 0dp untuk menggunakan weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f // Berat 1.0 untuk mengisi space horizontal
            ).apply {
                setMargins(0, 0, 20, 0) // Margin right 20dp
            }
            setBackgroundResource(R.drawable.input_shape) // Set background sebagai card
            setPadding(30, 30, 30, 30) // Padding
        }

        // EditText untuk Email
        val newMemberEditText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "Enter email"
            setTextColor(resources.getColor(R.color.tv_color, null))
            setHintTextColor(resources.getColor(R.color.gray, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            setTextSize(16f)

            // Listener ketika enter ditekan
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || event.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                    val emailInput = text.toString().trim()
                    if (emailInput.isNotEmpty()) {
                        fetchUsernameFromEmail(emailInput, horizontalLayout)
                    }
                    true
                } else {
                    false
                }
            }
        }

        // Tambahkan EditText ke dalam card email
        emailCard.addView(newMemberEditText)

        // Card untuk Username
        val usernameCard = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f // Berat 1.0 untuk mengisi space horizontal
            )
            setBackgroundResource(R.drawable.input_date) // Set background sebagai card
            setPadding(30, 30, 30, 30)
        }

        val memberNameTextView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "" // Username awalnya kosong
            setTextSize(16f)
            setTextColor(resources.getColor(R.color.bg, null))
            typeface = resources.getFont(R.font.poppinsmedium)

            // Tetapkan maxLines dan ellipsize agar card tetap konsisten
            maxLines = 2 // Batasi maksimal 2 baris
            ellipsize = TextUtils.TruncateAt.END // Jika terlalu panjang, potong dengan "..."
        }

        // Tambahkan TextView ke dalam card username
        usernameCard.addView(memberNameTextView)

        // Tambahkan card email dan card username ke layout horizontal
        horizontalLayout.addView(emailCard)
        horizontalLayout.addView(usernameCard)

        // Tambahkan baris baru ke container utama (linearLayoutContainerTeam)
        linearLayoutContainerTeam?.addView(horizontalLayout)
    }

    // Fungsi untuk mengambil username dari Firestore berdasarkan email
    private fun fetchUsernameFromEmail(email: String, horizontalLayout: LinearLayout) {
        val usernameCard = horizontalLayout.getChildAt(1) as LinearLayout // Card untuk username
        val memberNameTextView = usernameCard.getChildAt(0) as TextView // TextView untuk username di dalam card

        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val username = document.getString("username")
                        if (!username.isNullOrEmpty()) {
                            // Update TextView dengan username
                            memberNameTextView.text = username
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "No user found with this email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun createProject() {
        val projectTitle = view?.findViewById<EditText>(R.id.etProjectTitle)?.text.toString().trim()
        val projectDetail = view?.findViewById<EditText>(R.id.etProjectDetail)?.text.toString().trim()
        val dueDate = view?.findViewById<EditText>(R.id.etDateTime)?.text.toString().trim()
        val linearLayoutContainer: LinearLayout? = view?.findViewById(R.id.linearLayoutContainer)
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)

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

        // Clear memberList first, and retrieve all member input values from the EditTexts
        memberList.clear()
        linearLayoutContainerTeam?.let {
            for (i in 0 until it.childCount) {
                val horizontalLayout = it.getChildAt(i) as? LinearLayout
                val emailEditText = horizontalLayout?.getChildAt(0) as? EditText // Ambil EditText pertama untuk email
                emailEditText?.text?.toString()?.let { email ->
                    if (email.isNotEmpty()) {
                        memberList.add(email) // Masukkan email ke dalam memberList
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
            "dueDate" to dueDate,
            "taskList" to taskList,
            "memberList" to memberList, // Save member list
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
