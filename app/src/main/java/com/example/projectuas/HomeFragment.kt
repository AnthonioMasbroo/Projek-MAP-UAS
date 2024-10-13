package com.example.projectuas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val tvWelcome: TextView = view.findViewById(R.id.tvWelcome)

        // Get the username passed from LoginFragment
        val username = arguments?.getString("username")

        // Display the username in the welcome message
        tvWelcome.text = "Welcome, $username"

        return view
    }
}
