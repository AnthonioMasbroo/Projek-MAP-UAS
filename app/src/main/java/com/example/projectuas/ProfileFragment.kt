package com.example.projectuas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.example.projectuas.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Setup click listeners and other logic here

        // Handle Edit Profile click
        val editProfileLayout: LinearLayout = view.findViewById(R.id.editProfileLayout)
        editProfileLayout.setOnClickListener {
            // Example: You can launch an activity for editing the profile
            val intent = Intent(requireActivity(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Handle Logout click
        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            // Handle logout logic here, for example:
            // Clear user session and navigate to login screen
        }

        // Handle Notification switch toggle
        val notificationSwitch: Switch = view.findViewById(R.id.notificationSwitch)
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Handle notification toggle (enable or disable notifications)
        }

        return view
    }
}
