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
import android.util.Log

open class AddProjectFragment : Fragment(R.layout.fragment_add_project) {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var taskList: MutableList<String>
    private lateinit var memberList: MutableList<String>

    // Changed to protected as per suggestion
    protected var currentProject: Project? = null
    protected var isEditMode = false
    private var currentUserEmail: String? = null
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

    private fun initializeComponents(view: View) {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        taskList = mutableListOf()
        memberList = mutableListOf()
        currentUserEmail = auth.currentUser?.email
        btnAddProject = view.findViewById(R.id.btnAddProject)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize components
        initializeComponents(view)

        // Retrieve UI elements
        val addTaskButton: LinearLayout = view.findViewById(R.id.addTaskButton)
        val ivDateTime: ImageView = view.findViewById(R.id.ivDateTime)
        val etDateTime: EditText = view.findViewById(R.id.etDateTime)
        val ivAddTeamMember: ImageView = view.findViewById(R.id.ivAddTeamMember)

        // Handle arguments for edit mode
        arguments?.let { args ->
            isEditMode = args.getBoolean("isEditMode", false)
            val projectData = args.getParcelable<Project>("projectData")

            Log.d("AddProjectFragment", "IsEditMode: $isEditMode")
            Log.d("AddProjectFragment", "Project Data: $projectData")

            if (isEditMode && projectData != null) {
                currentProject = projectData
                fillProjectFields(projectData)
                Log.d("AddProjectFragment", "Edit mode activated with project: $projectData")
            }
        }

        // Set button text based on mode
        btnAddProject.text = if (isEditMode) "Update Project" else "Create Project"
        Log.d("AddProjectFragment", "Fragment initialized in ${if (isEditMode) "edit" else "create"} mode")

        // Handle add task button
        addTaskButton.setOnClickListener {
            if (canAddNewTaskField()) {
                addTaskField()
            } else {
                Toast.makeText(requireContext(), "Harap isi task sebelumnya terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle add member button (icon)
        ivAddTeamMember.setOnClickListener {
            if (canAddNewMemberField()) {
                addMemberField()
            } else {
                Toast.makeText(requireContext(), "Harap isi email member sebelumnya terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle create/edit project button
        btnAddProject.setOnClickListener {
            createProject()
        }

        // Handle calendar icon click
        ivDateTime.setOnClickListener {
            showDatePickerDialog(etDateTime)
        }
    }

    private fun canAddNewTaskField(): Boolean {
        val linearLayoutContainer: LinearLayout? = view?.findViewById(R.id.linearLayoutContainer)
        if (linearLayoutContainer == null || linearLayoutContainer.childCount == 0) {
            return true
        }

        val lastTaskLayout = linearLayoutContainer.getChildAt(linearLayoutContainer.childCount - 1) as? LinearLayout
        val taskEditText = lastTaskLayout?.getChildAt(0) as? EditText

        return taskEditText?.text?.isNotEmpty() == true
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

        // Create horizontal layout for Task and Delete Button
        val taskLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
        }

        // EditText for Task
        val newTaskEditText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ).apply {
                setMargins(0, 0, 20, 0)
            }
            hint = "Task"
            setBackgroundResource(R.drawable.input_date)
            setPadding(50, 50, 20, 50)
            setTextColor(resources.getColor(R.color.bg, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            setTextSize(20f)
        }

        // Delete Button
        val btnDeleteTask = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_delete) // Ensure you have a delete icon in drawable
            background = null
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                linearLayoutContainer?.removeView(taskLayout)
            }
        }

        taskLayout.addView(newTaskEditText)
        taskLayout.addView(btnDeleteTask)
        linearLayoutContainer?.addView(taskLayout)
    }

    @SuppressLint("NewApi")
    private fun addMemberField() {
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)

        // Check if the last field is filled
        if (linearLayoutContainerTeam != null && linearLayoutContainerTeam.childCount > 0) {
            val lastMemberLayout = linearLayoutContainerTeam.getChildAt(linearLayoutContainerTeam.childCount - 1) as LinearLayout
            val emailEditText = (lastMemberLayout.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? EditText

            if (emailEditText?.text.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Harap isi email member sebelumnya terlebih dahulu", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Create horizontal layout for Email, Username, and Delete Button
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
        }

        // Card for Email
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

        // EditText for Email
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

            // Listener for Enter key
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                ) {
                    val emailInput = text.toString().trim()
                    if (emailInput.isNotEmpty()) {
                        val currentEmails = getCurrentEnteredEmails()
                        if (currentEmails.filter { it.equals(emailInput, ignoreCase = true) }.size > 1) {
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

        emailCard.addView(newMemberEditText)

        // Card for Username
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

        // Delete Button
        val btnDeleteMember = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_delete) // Ensure you have a delete icon in drawable
            background = null
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                linearLayoutContainerTeam?.removeView(horizontalLayout)
            }
        }

        horizontalLayout.addView(emailCard)
        horizontalLayout.addView(usernameCard)
        horizontalLayout.addView(btnDeleteMember)

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
                            // Do not add to memberList here
                            // Addition is done in createProject() after validation
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

        // Validate input
        if (projectTitle.isEmpty() || projectDetail.isEmpty() || dueDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear and retrieve all task values
        taskList.clear()
        linearLayoutContainer?.let {
            for (i in 0 until it.childCount) {
                val taskLayout = it.getChildAt(i) as? LinearLayout
                val taskEditText = taskLayout?.getChildAt(0) as? EditText
                val task = taskEditText?.text.toString().trim()
                if (task.isNotEmpty()) {
                    taskList.add(task)
                }
            }
        }

        // Clear and retrieve all member values
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

    private fun getCurrentEnteredEmails(): List<String> {
        val emails = mutableListOf<String>()
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)
        linearLayoutContainerTeam?.let {
            for (i in 0 until it.childCount) {
                val horizontalLayout = it.getChildAt(i) as? LinearLayout
                val emailCard = horizontalLayout?.getChildAt(0) as? LinearLayout
                val emailEditText = emailCard?.getChildAt(0) as? EditText
                val email = emailEditText?.text.toString().trim()
                if (email.isNotEmpty()) {
                    emails.add(email)
                }
            }
        }
        return emails
    }

    @SuppressLint("NewApi")
    protected fun fillProjectFields(project: Project) { // Changed to protected as per suggestion
        try {
            Log.d("AddProjectFragment", "Filling project fields: $project")
            view?.findViewById<EditText>(R.id.etProjectTitle)?.setText(project.projectTitle)
            view?.findViewById<EditText>(R.id.etProjectDetail)?.setText(project.projectDetail)
            view?.findViewById<EditText>(R.id.etDateTime)?.setText(project.dueDate)

            // Clear existing fields
            view?.findViewById<LinearLayout>(R.id.linearLayoutContainer)?.removeAllViews()
            view?.findViewById<LinearLayout>(R.id.linearLayoutContainerTeam)?.removeAllViews()

            // Fill tasks
            project.taskList.forEach { task ->
                addTaskFieldWithText(task)
            }

            // Fill members
            project.memberList.forEach { member ->
                val parts = member.split(" (")
                val email = parts[0]
                val username = parts.getOrNull(1)?.removeSuffix(")") ?: ""
                addMemberFieldWithText(email, username)
            }

            Log.d("AddProjectFragment", "Successfully filled all project fields")
        } catch (e: Exception) {
            Log.e("AddProjectFragment", "Error filling project fields", e)
            Toast.makeText(context, "Error loading project data", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NewApi")
    private fun addTaskFieldWithText(task: String) {
        val linearLayoutContainer: LinearLayout? = view?.findViewById(R.id.linearLayoutContainer)

        // Create horizontal layout for Task and Delete Button
        val taskLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
        }

        // EditText for Task
        val newTaskEditText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ).apply {
                setMargins(0, 0, 20, 0)
            }
            hint = "Task"
            setBackgroundResource(R.drawable.input_date)
            setPadding(50, 50, 20, 50)
            setTextColor(resources.getColor(R.color.bg, null))
            typeface = resources.getFont(R.font.poppinsmedium)
            setTextSize(20f)
            setText(task)
        }

        // Delete Button
        val btnDeleteTask = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_delete) // Ensure you have a delete icon in drawable
            background = null
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                linearLayoutContainer?.removeView(taskLayout)
            }
        }

        taskLayout.addView(newTaskEditText)
        taskLayout.addView(btnDeleteTask)
        linearLayoutContainer?.addView(taskLayout)
    }

    @SuppressLint("NewApi")
    private fun addMemberFieldWithText(email: String, username: String) {
        val linearLayoutContainerTeam: LinearLayout? = view?.findViewById(R.id.linearLayoutContainerTeam)

        // Create horizontal layout for Email and Username
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
        }

        // Card for Email
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

        // EditText for Email
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
            isEnabled = false // Disable editing after pre-filling

            // Listener not needed when pre-filling data
        }

        emailCard.addView(newMemberEditText)

        // Card for Username
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

                // Update Project object with document ID
                val updatedProject = project.copy(documentId = documentRef.id)

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
