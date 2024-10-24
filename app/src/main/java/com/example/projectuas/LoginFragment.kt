package com.example.projectuas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etUsername: EditText = view.findViewById(R.id.etUsername)
        val etPassword: EditText = view.findViewById(R.id.etPassword)
        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        val tvRegister2: TextView = view.findViewById(R.id.tvRegister2)
        val ivTogglePassword: ImageView = view.findViewById(R.id.ivTogglePassword)

        // Error TextViews
        val tvEmailError: TextView = view.findViewById(R.id.tvEmailError)
        val tvPasswordError: TextView = view.findViewById(R.id.tvPasswordError)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            // Reset error messages
            tvEmailError.visibility = View.GONE
            tvPasswordError.visibility = View.GONE

            var isValid = true

            // Email validation
            if (username.isEmpty()) {
                tvEmailError.text = "Email is required"
                tvEmailError.visibility = View.VISIBLE
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
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

            if (isValid) {
                auth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // Fetch user data from Firestore
                                db.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val fetchedUsername = document.getString("username")
                                            val fetchedEmail = document.getString("email") // Misalnya kamu juga ingin simpan email

                                            if (fetchedUsername != null && fetchedEmail != null) {
                                                // Simpan username dan email ke SharedPreferences
                                                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                                                with(sharedPref.edit()) {
                                                    putString("username", fetchedUsername)
                                                    putString("email", fetchedEmail)
                                                    apply() // Simpan perubahan
                                                }

                                                // Panggil onLoginSuccess di MainActivity
                                                (activity as MainActivity).onLoginSuccess(fetchedUsername)
                                            } else {
                                                Toast.makeText(activity, "Username atau Email tidak ditemukan", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(activity, "Data pengguna tidak ditemukan", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(activity, "Gagal mengambil data pengguna", Toast.LENGTH_LONG).show()
                                    }
                            }
                        } else {
                            Toast.makeText(activity, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        tvRegister2.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .commit()
        }

        // Set initial state of password visibility
        ivTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                // Hide password
                etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.baseline_visibility_off_24) // Change icon
            } else {
                // Show password
                etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.baseline_visibility_24) // Change icon
            }

            // Move cursor to the end of the text after toggling
            etPassword.setSelection(etPassword.text.length)

            // Toggle the flag
            isPasswordVisible = !isPasswordVisible
        }

        return view
    }
}
