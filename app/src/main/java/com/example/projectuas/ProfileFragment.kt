package com.example.projectuas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.projectuas.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Ambil data dari SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "Guest User")
        val email = sharedPref.getString("email", "guest@example.com")
        val profileImageUri = sharedPref.getString("profileImageUri", null)

        // Set data ke UI
        val profileNameTextView: TextView = view.findViewById(R.id.profileName)
        val profileEmailTextView: TextView = view.findViewById(R.id.profileEmail)
        val profileImageView: ImageView = view.findViewById(R.id.profileImage)

        profileNameTextView.text = username
        profileEmailTextView.text = email

        // Tampilkan gambar profil jika tersedia
        profileImageUri?.let {
            Glide.with(this)
                .load(it)
                .into(profileImageView)
        }

        // Buka EditProfileActivity saat Edit Profile diklik
        val editProfileLayout: LinearLayout = view.findViewById(R.id.editProfileLayout)
        editProfileLayout.setOnClickListener {
            val intent = Intent(requireActivity(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Handle Logout click
        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            // Hapus data dari SharedPreferences saat logout
            with(sharedPref.edit()) {
                clear() // Hapus semua data
                apply()
            }

            val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigationView.visibility = View.GONE

            // Ganti fragment ke LoginFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment()) // fragment_container adalah ID dari layout container di MainActivity
                .commit()

            // Opsi: Tampilkan pesan logout berhasil
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
