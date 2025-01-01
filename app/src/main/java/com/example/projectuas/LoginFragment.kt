package com.example.projectuas

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible: Boolean = false
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("LoginFragment", "Google sign in successful, ID Token: ${account.idToken}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("LoginFragment", "Google sign in failed", e)
                Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Google sign in canceled.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Inisialisasi Firebase Auth dan Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inisialisasi UI Components
        val etUsername: EditText = view.findViewById(R.id.etUsername)
        val etPassword: EditText = view.findViewById(R.id.etPassword)
        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        val tvRegister2: TextView = view.findViewById(R.id.tvRegister2)
        val ivTogglePassword: ImageView = view.findViewById(R.id.ivTogglePassword)
        val btnGoogleSignIn: LinearLayout = view.findViewById(R.id.btnGoogleSignIn) // Tombol kustom
        val tvResetPassword: TextView = view.findViewById(R.id.tvResetPassword)

        // Error TextViews
        val tvEmailError: TextView = view.findViewById(R.id.tvEmailError)
        val tvPasswordError: TextView = view.findViewById(R.id.tvPasswordError)

        // Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Pastikan Anda memiliki default_web_client_id di strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Setup Google Sign-In button (kustom)
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // Setup Reset Password
        tvResetPassword.setOnClickListener {
            showResetPasswordDialog()
        }

        // Handle Login Button Click
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            Log.d("LoginFragment", "Attempting to sign in")

            // Reset error messages
            tvEmailError.visibility = View.GONE
            tvPasswordError.visibility = View.GONE

            var isValid = true

            // Validasi Email
            if (username.isEmpty()) {
                tvEmailError.text = "Email is required"
                tvEmailError.visibility = View.VISIBLE
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
                tvEmailError.text = "Invalid email format"
                tvEmailError.visibility = View.VISIBLE
                isValid = false
            }

            // Validasi Password
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
                                            val fetchedEmail = document.getString("email")
                                            val fetchedProfileImageUrl = document.getString("profileImageUrl")

                                            if (fetchedUsername != null && fetchedEmail != null) {
                                                // Simpan username, email, dan profileImageUrl ke SharedPreferences
                                                val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                                                with(sharedPref.edit()) {
                                                    putString("username", fetchedUsername)
                                                    putString("email", fetchedEmail)
                                                    putString("profileImageUri", fetchedProfileImageUrl) // Simpan URL gambar profil
                                                    apply()
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

        // Navigate to Register Fragment
        tvRegister2.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .commit()
        }

        // Toggle Password Visibility
        ivTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                // Sembunyikan password
                etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.baseline_visibility_off_24) // Ubah ikon sesuai
            } else {
                // Tampilkan password
                etPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.baseline_visibility_24) // Ubah ikon sesuai
            }

            // Pindahkan kursor ke akhir teks setelah toggling
            etPassword.setSelection(etPassword.text.length)

            // Toggle flag
            isPasswordVisible = !isPasswordVisible
        }

        return view
    }

    // Method untuk memulai proses Google Sign-In dengan meminta pemilihan akun setiap kal
    private fun signInWithGoogle() {
        // Pastikan Google SignInClient dalam keadaan baru (memaksa pemilihan akun)
        googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
            // Memulai proses SignIn kembali setelah logout
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    // Mengautentikasi dengan Firebase menggunakan token Google
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("LoginFragment", "signInWithCredential:success")
                    val user = auth.currentUser
                    saveUserToFirestore(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("LoginFragment", "signInWithCredential:failure", task.exception)
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Menyimpan data pengguna ke Firestore dan SharedPreferences
    private fun saveUserToFirestore(user: FirebaseUser?) {
        user?.let {
            val userMap = hashMapOf(
                "username" to user.displayName,
                "email" to user.email,
                "profileImageUrl" to user.photoUrl?.toString()
            )

            db.collection("users").document(user.uid)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("LoginFragment", "User data successfully written!")
                    // Simpan ke SharedPreferences
                    val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("username", user.displayName)
                        putString("email", user.email)
                        putString("profileImageUri", user.photoUrl?.toString())
                        apply()
                    }
                    // Panggil onLoginSuccess di MainActivity
                    (activity as MainActivity).onLoginSuccess(user.displayName ?: "User")
                }
                .addOnFailureListener { e ->
                    Log.w("LoginFragment", "Error writing user data", e)
                    Toast.makeText(context, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Menampilkan dialog untuk mereset password
    private fun showResetPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val etEmail = view.findViewById<EditText>(R.id.etResetEmail)

        builder.setView(view)
            .setTitle("Reset Password")
            .setPositiveButton("Reset") { dialog, _ ->
                val email = etEmail.text.toString().trim()
                if (email.isNotEmpty()) {
                    resetPassword(email)
                } else {
                    Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Mengirim email reset password melalui Firebase
    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Reset password email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onStop() {
        super.onStop()
        // Logout dari Google Sign-In ketika fragment dihentikan
        googleSignInClient.signOut()
    }

}
