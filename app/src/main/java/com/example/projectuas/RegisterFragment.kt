package com.example.projectuas

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        auth = FirebaseAuth.getInstance()
        var db = FirebaseFirestore.getInstance()

        val etUsername: EditText = view.findViewById(R.id.etUsername)
        val etEmail: EditText = view.findViewById(R.id.etEmail)
        val etPassword: EditText = view.findViewById(R.id.etPassword)
        val etConfirmPassword: EditText = view.findViewById(R.id.etConfirmPassword)
        val btnRegister: Button = view.findViewById(R.id.btnRegister)
        val tvLogin2: TextView = view.findViewById(R.id.tvLogin2)
        val ivTogglePassword: ImageView = view.findViewById(R.id.ivTogglePassword)
        val ivTogglePassword2: ImageView = view.findViewById(R.id.ivTogglePassword2)

        // Error TextViews
        val tvUsernameError: TextView = view.findViewById(R.id.tvUsernameError)
        val tvEmailError: TextView = view.findViewById(R.id.tvEmailError)
        val tvPasswordError: TextView = view.findViewById(R.id.tvPasswordError)
        val tvConfirmPasswordError: TextView = view.findViewById(R.id.tvConfirmPasswordError)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Reset error messages
            tvUsernameError.visibility = View.GONE
            tvEmailError.visibility = View.GONE
            tvPasswordError.visibility = View.GONE
            tvConfirmPasswordError.visibility = View.GONE

            var isValid = true

            // Username validation
            if (username.isEmpty()) {
                tvUsernameError.text = "Username is required"
                tvUsernameError.visibility = View.VISIBLE
                isValid = false
            }

            // Email validation
            if (email.isEmpty()) {
                tvEmailError.text = "Email is required"
                tvEmailError.visibility = View.VISIBLE
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvEmailError.text = "Invalid email format"
                tvEmailError.visibility = View.VISIBLE
                isValid = false
            }

            // Password validation
            if (password.isEmpty()) {
                tvPasswordError.text = "Password is required"
                tvPasswordError.visibility = View.VISIBLE
                isValid = false
            } else if (password.length < 8) {
                tvPasswordError.text = "Password must be at least 8 characters"
                tvPasswordError.visibility = View.VISIBLE
                isValid = false
            }

            // Confirm password validation
            if (isValid) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Save username to Firestore
                            val userId = auth.currentUser?.uid
                            val userMap = hashMapOf(
                                "username" to username,
                                "email" to email
                            )

                            // Save the user's username to Firestore
                            if (userId != null) {
                                db.collection("users").document(userId)
                                    .set(userMap)
                                    .addOnSuccessListener {
                                        parentFragmentManager.beginTransaction()
                                            .replace(R.id.fragment_container, LoginFragment())
                                            .commit()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(activity, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        } else {
                            Toast.makeText(activity, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        tvLogin2.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }


        // Set initial state of password visibility
        ivTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                // Hide password
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.baseline_visibility_off_24) // Ubah ikon
            } else {
                // Show password
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.baseline_visibility_24) // Ubah ikon
            }

            // Move cursor to the end of the text after toggling
            etPassword.setSelection(etPassword.text.length)

            // Toggle the flag
            isPasswordVisible = !isPasswordVisible
        }

        // Set initial state of password visibility
        ivTogglePassword2.setOnClickListener {
            if (isPasswordVisible) {
                // Hide password
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword2.setImageResource(R.drawable.baseline_visibility_off_24) // Ubah ikon
            } else {
                // Show password
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword2.setImageResource(R.drawable.baseline_visibility_24) // Ubah ikon
            }

            // Move cursor to the end of the text after toggling
            etConfirmPassword.setSelection(etConfirmPassword.text.length)

            // Toggle the flag
            isPasswordVisible = !isPasswordVisible
        }

        return view
    }
}
