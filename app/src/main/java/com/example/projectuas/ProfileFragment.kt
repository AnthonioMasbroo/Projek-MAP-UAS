package com.example.projectuas

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {

    private lateinit var notificationSwitch: Switch
    private var sharedPref: SharedPreferences? = null

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
                .circleCrop()
                .into(profileImageView)
        }

        // Setup notification switch
        setupNotificationSwitch(view)

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

            // Cancel semua WorkManager tasks
            WorkManager.getInstance(requireContext()).cancelAllWork()

            val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigationView.visibility = View.GONE

            // Ganti fragment ke LoginFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()

            // Tampilkan pesan logout berhasil
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        }

        // Tambahkan logika untuk backButton
        val backButton: ImageView = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            // Navigasi kembali ke HomeFragment dengan mengatur item yang dipilih di BottomNavigationView
            val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigationView.selectedItemId = R.id.nav_home
        }

        return view
    }

    private fun setupNotificationSwitch(view: View) {
        notificationSwitch = view.findViewById(R.id.notificationSwitch)
        sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Load saved state
        notificationSwitch.isChecked = sharedPref?.getBoolean("notifications_enabled", false) ?: false

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref?.edit()?.putBoolean("notifications_enabled", isChecked)?.apply()
            scheduleDeadlineChecks(isChecked)
        }
    }

    private fun scheduleDeadlineChecks(isEnabled: Boolean) {
        if (isEnabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val deadlineCheckRequest = PeriodicWorkRequestBuilder<DeadlineCheckWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(requireContext())
                .enqueueUniquePeriodicWork(
                    "deadline_check",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    deadlineCheckRequest
                )

            // Buat channel notifikasi
            NotificationHelper.createNotificationChannel(requireContext())

            Toast.makeText(requireContext(),
                "Notifikasi deadline diaktifkan",
                Toast.LENGTH_SHORT).show()
        } else {
            WorkManager.getInstance(requireContext())
                .cancelUniqueWork("deadline_check")

            Toast.makeText(requireContext(),
                "Notifikasi deadline dinonaktifkan",
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh profil data ketika kembali ke fragment
        sharedPref = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "Guest User")
        val email = sharedPref?.getString("email", "guest@example.com")
        val profileImageUri = sharedPref?.getString("profileImageUri", null)

        view?.let { view ->
            view.findViewById<TextView>(R.id.profileName).text = username
            view.findViewById<TextView>(R.id.profileEmail).text = email

            profileImageUri?.let {
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(view.findViewById(R.id.profileImage))
            }
        }
    }
}