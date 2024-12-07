package com.example.projectuas

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.projectuas.models.Project
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import android.text.Editable
import android.text.TextWatcher

class AddProjectFragment : Fragment(R.layout.fragment_add_project) {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var taskList: MutableList<String>
    private lateinit var memberList: MutableList<String>
    private var currentProject: Project? = null
    private var currentUserEmail: String? = null
    private var isEditMode = false
    private lateinit var btnAddProject: Button


    private val projectDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data
            if (data != null && data.hasExtra("projectData")) {
                currentProject = data.getParcelableExtra<Project>("projectData")
                currentProject?.let { project ->
                    fillProjectFields(project)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        taskList = mutableListOf()
        memberList = mutableListOf()

        currentUserEmail = auth.currentUser?.email

        val addTaskButton: LinearLayout = view.findViewById(R.id.addTaskButton)
        val btnAddProject: Button = view.findViewById(R.id.btnAddProject)
        val ivDateTime: ImageView = view.findViewById(R.id.ivDateTime)
        val etDateTime: EditText = view.findViewById(R.id.etDateTime)
        val ivAddTeamMember: ImageView = view.findViewById(R.id.ivAddTeamMember)



        // Cek apakah dalam mode edit
        arguments?.getParcelable<Project>("projectData")?.let { project ->
            currentProject = project
            isEditMode = true
            fillProjectFields(project)
            // Ubah text button sesuai mode
            btnAddProject.text = "Edit Project"
        } ?: run {
            isEditMode = false
            btnAddProject.text = "Create Project"
        }

        // Handle add task button
        addTaskButton.setOnClickListener {
            addTaskField()
        }

        // Handle add member button (icon)
        ivAddTeamMember.setOnClickListener {
            if (canAddNewMemberField()) {
                addMemberField()
            } else {
                Toast.makeText(requireContext(), "Harap isi email member sebelumnya terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
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

    private fun canAddNewMemberField(): Boolean {
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)
        if (linearLayoutContainerTeam == null || linearLayoutContainerTeam.childCount == 0) {
            return true
        }

        val lastMemberLayout = linearLayoutContainerTeam.getChildAt(linearLayoutContainerTeam.childCount - 1) as? LinearLayout
        val emailCard = lastMemberLayout?.getChildAt(0) as? LinearLayout
        val emailEditText = emailCard?.getChildAt(0) as? EditText

        return emailEditText?.text?.isNotEmpty() == true
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
                setMargins(0, 20, 0, 0)
            }
            hint = "Task"
            setBackgroundResource(R.drawable.input_date)
            setPadding(50, 50, 20, 50)
            setTextColor(resources.getColor(R.color.bg, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            setTextSize(20f)
        }
        linearLayoutContainer?.addView(newTaskEditText)
    }

    @SuppressLint("NewApi")
    private fun addMemberField() {
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)

        // Cek apakah field terakhir telah diisi
        if (linearLayoutContainerTeam != null && linearLayoutContainerTeam.childCount > 0) {
            val lastMemberLayout = linearLayoutContainerTeam.getChildAt(linearLayoutContainerTeam.childCount - 1) as LinearLayout
            val emailEditText = lastMemberLayout.getChildAt(0) as? EditText
            val usernameTextView = lastMemberLayout.getChildAt(1) as? TextView

            val email = emailEditText?.text.toString().trim()
            val username = usernameTextView?.text.toString().trim()

            if (email.isEmpty() || username.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill out the last member's email before adding a new one.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Buat layout horizontal untuk Email dan Username
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
        }

        // Card untuk Email
        val emailCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ).apply {
                setMargins(0, 0, 20, 0)
            }
            setBackgroundResource(R.drawable.input_shape)
            setPadding(30, 30, 30, 30)
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
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                ) {
                    val emailInput = text.toString().trim()
                    if (emailInput.isNotEmpty()) {
                        if (getAddedEmails().contains(emailInput)) {
                            Toast.makeText(requireContext(), "Email sudah ditambahkan", Toast.LENGTH_SHORT).show()
                        } else if (emailInput.equals(currentUserEmail, ignoreCase = true)) {
                            Toast.makeText(requireContext(), "Anda tidak dapat menambahkan diri sendiri", Toast.LENGTH_SHORT).show()
                        } else {
                            fetchUsernameFromEmail(emailInput, horizontalLayout)
                        }
                    }
                    true
                } else {
                    false
                }
            }
        }

        newMemberEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Jika EditText tidak kosong, aktifkan tombol tambah anggota
                // (Jika ada tombol tambah anggota terpisah)
            }
        })

        emailCard.addView(newMemberEditText)

        // Card untuk Username
        val usernameCard = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            setBackgroundResource(R.drawable.input_date)
            setPadding(30, 30, 30, 30)
        }

        val memberNameTextView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = ""
            setTextSize(16f)
            setTextColor(resources.getColor(R.color.bg, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }

        usernameCard.addView(memberNameTextView)

        horizontalLayout.addView(emailCard)
        horizontalLayout.addView(usernameCard)

        linearLayoutContainerTeam?.addView(horizontalLayout)
    }

    private fun getAddedEmails(): List<String> {
        return memberList.map { member ->
            member.substringBefore(" (").trim()
        }
    }

    private fun fetchUsernameFromEmail(email: String, horizontalLayout: LinearLayout) {
        // Reference to the username TextView
        val usernameCard = horizontalLayout.getChildAt(1) as LinearLayout
        val memberNameTextView = usernameCard.getChildAt(0) as TextView

        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val username = document.getString("username")
                        if (!username.isNullOrEmpty()) {
                            memberNameTextView.text = username
                            // Tidak menambahkan ke memberList di sini
                            // Penambahan dilakukan di createProject() setelah validasi
                        } else {
                            memberNameTextView.text = "Unknown"
                            Toast.makeText(requireContext(), "Username not found for $email", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    memberNameTextView.text = "Unknown"
                    Toast.makeText(requireContext(), "No user found with this email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                memberNameTextView.text = "Error"
                Toast.makeText(requireContext(), "Error fetching user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createProject() {
        val projectTitle = view?.findViewById<EditText>(R.id.etProjectTitle)?.text.toString().trim()
        val projectDetail = view?.findViewById<EditText>(R.id.etProjectDetail)?.text.toString().trim()
        val dueDate = view?.findViewById<EditText>(R.id.etDateTime)?.text.toString().trim()
        val linearLayoutContainer: LinearLayout? = view?.findViewById(R.id.linearLayoutContainer)
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)

        // Validasi input
        if (projectTitle.isEmpty() || projectDetail.isEmpty() || dueDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear dan ambil semua nilai tugas
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

        // Clear dan ambil semua nilai anggota
        memberList.clear()
        var allMembersValid = true
        linearLayoutContainerTeam?.let {
            for (i in 0 until it.childCount) {
                val horizontalLayout = it.getChildAt(i) as? LinearLayout
                val emailCard = horizontalLayout?.getChildAt(0) as? LinearLayout
                val emailEditText = emailCard?.getChildAt(0) as? EditText
                val usernameCard = horizontalLayout?.getChildAt(1) as? LinearLayout
                val usernameTextView = usernameCard?.getChildAt(0) as? TextView

                val email = emailEditText?.text.toString().trim()
                val username = usernameTextView?.text.toString().trim()

                if (email.isNotEmpty() && username.isNotEmpty() && username != "Unknown") {
                    val member = "$email ($username)"
                    if (!memberList.contains(member)) {
                        memberList.add(member)
                    }
                } else if (email.isNotEmpty()) {
                    allMembersValid = false
                    break
                }
            }
        }

        if (!allMembersValid) {
            Toast.makeText(requireContext(), "Please ensure all member emails are valid and usernames are fetched.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Create project object
        val documentId = if (isEditMode) currentProject?.documentId else ""
        val project = Project(
            projectTitle = projectTitle,
            projectDetail = projectDetail,
            dueDate = dueDate,
            taskList = taskList,
            memberList = memberList,
            userId = currentUser.uid,
            documentId = documentId ?: ""
        )

        // Save to Firestore and navigate
        saveProjectToFirestore(project)
    }


    @SuppressLint("NewApi")
    private fun fillProjectFields(project: Project) {
        // Isi EditTexts dengan data project
        view?.findViewById<EditText>(R.id.etProjectTitle)?.setText(project.projectTitle)
        view?.findViewById<EditText>(R.id.etProjectDetail)?.setText(project.projectDetail)
        view?.findViewById<EditText>(R.id.etDateTime)?.setText(project.dueDate)

        // Hapus task yang ada dan tambahkan yang baru
        val linearLayoutContainer: LinearLayout? = view?.findViewById(R.id.linearLayoutContainer)
        linearLayoutContainer?.removeAllViews()
        project.taskList.forEach { task ->
            addTaskFieldWithText(task)
        }

        // Hapus member yang ada dan tambahkan yang baru
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)
        linearLayoutContainerTeam?.removeAllViews()
        project.memberList.forEach { member ->
            val memberParts = member.split(" (")
            val email = memberParts.getOrNull(0) ?: ""
            val username = memberParts.getOrNull(1)?.removeSuffix(")") ?: ""

            addMemberFieldWithText(email, username)
        }
    }

    @SuppressLint("NewApi")
    private fun addTaskFieldWithText(task: String) {
        val linearLayoutContainer: LinearLayout? = view?.findViewById(R.id.linearLayoutContainer)
        val newTaskEditText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
            hint = "Task"
            setBackgroundResource(R.drawable.input_date)
            setPadding(50, 50, 20, 50)
            setTextColor(resources.getColor(R.color.bg, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            setTextSize(20f)
            setText(task)
        }
        linearLayoutContainer?.addView(newTaskEditText)
    }

    @SuppressLint("NewApi")
    private fun addMemberFieldWithText(email: String, username: String) {
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)

        // Buat layout horizontal untuk Email dan Username
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
        }

        // Card untuk Email
        val emailCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ).apply {
                setMargins(0, 0, 20, 0)
            }
            setBackgroundResource(R.drawable.input_shape)
            setPadding(30, 30, 30, 30)
        }

        // EditText untuk Email
        val newMemberEditText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "Enter email"
            setText(email)
            setTextColor(resources.getColor(R.color.tv_color, null))
            setHintTextColor(resources.getColor(R.color.gray, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            setTextSize(16f)
            isEnabled = false // Nonaktifkan edit agar tidak diubah setelah diisi

            // Listener ketika enter ditekan (tidak diperlukan saat mengisi dari data yang ada)
        }

        emailCard.addView(newMemberEditText)

        // Card untuk Username
        val usernameCard = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            setBackgroundResource(R.drawable.input_date)
            setPadding(30, 30, 30, 30)
        }

        val memberNameTextView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = username
            setTextSize(16f)
            setTextColor(resources.getColor(R.color.bg, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }

        usernameCard.addView(memberNameTextView)

        horizontalLayout.addView(emailCard)
        horizontalLayout.addView(usernameCard)

        linearLayoutContainerTeam?.addView(horizontalLayout)
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

    private fun saveProjectToFirestore(project: Project) {
        val documentRef = if (isEditMode && currentProject?.documentId?.isNotEmpty() == true) {
            // Update existing document
            firestore.collection("projects").document(currentProject!!.documentId)
        } else {
            // Create new document
            firestore.collection("projects").document()
        }

        val projectData = hashMapOf(
            "projectTitle" to project.projectTitle,
            "projectDetail" to project.projectDetail,
            "dueDate" to project.dueDate,
            "taskList" to project.taskList,
            "memberList" to project.memberList,
            "userId" to project.userId
        )

        documentRef.set(projectData)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    if (isEditMode) "Project updated successfully" else "Project created successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Create updated Project object with document ID
                val updatedProject = Project(
                    projectTitle = project.projectTitle,
                    projectDetail = project.projectDetail,
                    dueDate = project.dueDate,
                    taskList = project.taskList,
                    memberList = project.memberList,
                    userId = project.userId,
                    documentId = documentRef.id
                )

                // Navigate to ProjectDetailActivity
                val intent = Intent(requireContext(), ProjectDetailActivity::class.java)
                intent.putExtra("projectData", updatedProject)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to ${if (isEditMode) "update" else "create"} project: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}