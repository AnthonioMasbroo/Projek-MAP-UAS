package com.example.projectuas

import android.content.Context
import com.google.firebase.FirebaseApp
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.projectuas.models.Project
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        bottomNavigationView = findViewById(R.id.bottom_navigation)


        // Tangani intent untuk navigasi ke HomeFragment
        if (intent.getBooleanExtra("RETURN_TO_HOME", false)) {
            loadFragment(HomeFragment())
            bottomNavigationView.visibility = View.VISIBLE
            bottomNavigationView.selectedItemId = R.id.nav_home
        } else if (savedInstanceState == null) {
            // Tampilkan LoginFragment sebagai fragment default
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
            bottomNavigationView.visibility = BottomNavigationView.GONE
        }

        // Handle intent extras untuk edit project
        if (intent.getStringExtra("openFragment") == "add_project") {
            val projectData = intent.getParcelableExtra<Project>("projectData")
            val addProjectFragment = AddProjectFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("projectData", projectData)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, addProjectFragment)
                .commit()
            bottomNavigationView.selectedItemId = R.id.nav_create
        }

        // Cek apakah ada intent untuk navigasi ke HomeFragment
        if (intent.getStringExtra("navigateTo") == "HomeFragment") {
            loadFragment(HomeFragment())
            bottomNavigationView.visibility = View.VISIBLE
            bottomNavigationView.selectedItemId = R.id.nav_home
        } else if (savedInstanceState == null) {
            // Tampilkan LoginFragment sebagai fragment default
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        // Awalnya sembunyikan BottomNavigationView sampai login sukses
        bottomNavigationView.visibility = BottomNavigationView.GONE

        if (savedInstanceState == null) {
            // Tampilkan LoginFragment sebagai fragment default
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        // Handle intent extras untuk edit project
        if (intent.getStringExtra("openFragment") == "add_project") {
            val projectData = intent.getParcelableExtra<Project>("projectData")
            val addProjectFragment = AddProjectFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("projectData", projectData)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, addProjectFragment)
                .commit()
            bottomNavigationView.selectedItemId = R.id.nav_create
        }

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

    // Fungsi ini dipanggil ketika login sukses dan menerima username
    fun onLoginSuccess(username: String) {
        // Simpan username di SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", username)
            apply() // Simpan perubahan
        }

        // Buat HomeFragment tanpa perlu kirim bundle, username diambil dari SharedPreferences
        loadFragment(HomeFragment())

        // Tampilkan BottomNavigationView
        bottomNavigationView.visibility = BottomNavigationView.VISIBLE
    }

    // Fungsi untuk mengganti fragment
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss()

        if (fragment is HomeFragment) {
            bottomNavigationView.visibility = View.VISIBLE
        } else {
            bottomNavigationView.visibility = View.VISIBLE // Sesuaikan jika diperlukan
        }
    }

    override fun onResume() {
        super.onResume()
        // Tampilkan BottomNavigation saat kembali ke HomeFragment
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) is HomeFragment) {
            bottomNavigationView.visibility = View.VISIBLE
        }
    }
}
