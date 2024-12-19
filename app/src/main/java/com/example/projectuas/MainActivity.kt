package com.example.projectuas

import android.content.Context
import android.content.Intent
import com.google.firebase.FirebaseApp
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.projectuas.models.Project
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        setupNavigation(savedInstanceState)

        // Setup listener untuk BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_create -> {
                    loadFragment(AddProjectFragment())
                    true
                }
                R.id.nav_archive -> {
                    loadFragment(ArchiveFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setupNavigation(null)
    }

    fun onLoginSuccess(username: String) {
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", username)
            apply()
        }
        loadFragment(HomeFragment())
        bottomNavigationView.visibility = View.VISIBLE
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        val mode = intent.getStringExtra("MODE")
        val isEditMode = intent.getBooleanExtra("isEditMode", false)
        Log.d("MainActivity", "Navigation mode: $mode")

        when (mode) {
            "EDIT_PROJECT" -> {
                val projectData = intent.getParcelableExtra<Project>("projectData")
                if (projectData != null && isEditMode) {
                    val fragment = EditProjectFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable("projectData", projectData)
                            putBoolean("isEditMode", true)
                        }
                    }
                    loadFragment(fragment)
                    bottomNavigationView.visibility = View.VISIBLE
                    // Penting: Jangan set selectedItemId disini
                    Log.d("MainActivity", "Loaded EditProjectFragment with edit mode")
                } else {
                    Toast.makeText(this, "Invalid edit mode state", Toast.LENGTH_SHORT).show()
                    loadFragment(HomeFragment())
                }
            }
            "HOME" -> {
                loadFragment(HomeFragment())
                bottomNavigationView.visibility = View.VISIBLE
                bottomNavigationView.selectedItemId = R.id.nav_home
            }
            null -> {
                if (savedInstanceState == null) {
                    loadFragment(LoginFragment())
                    bottomNavigationView.visibility = View.GONE
                }
            }
        }
    }


    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss()
    }

    override fun onResume() {
        super.onResume()
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) is HomeFragment) {
            bottomNavigationView.visibility = View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isEditMode", intent.getBooleanExtra("isEditMode", false))
        outState.putParcelable("projectData", intent.getParcelableExtra<Project>("projectData"))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.getBoolean("isEditMode", false)) {
            val projectData = savedInstanceState.getParcelable<Project>("projectData")
            if (projectData != null) {
                setupEditMode(projectData)
            }
        }
    }

    private fun setupEditMode(projectData: Project) {
        val fragment = EditProjectFragment().apply {
            arguments = Bundle().apply {
                putParcelable("projectData", projectData)
                putBoolean("isEditMode", true)
            }
        }
        loadFragment(fragment)
    }
}