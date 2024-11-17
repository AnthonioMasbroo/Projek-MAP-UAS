package com.example.projectuas

import android.content.Context
import com.google.firebase.FirebaseApp
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Awalnya sembunyikan BottomNavigationView sampai login sukses
        bottomNavigationView.visibility = BottomNavigationView.GONE

        if (savedInstanceState == null) {
            // Tampilkan LoginFragment sebagai fragment default
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        // Cek jika ada flag untuk navigasi ke fragment
        if (intent.getStringExtra("navigateTo") == "HomeFragment") {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())  // Pastikan container ID benar
                .commit()
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
        // Replace fragment in container
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Tampilkan BottomNavigation saat kembali ke HomeFragment
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) is HomeFragment) {
            bottomNavigationView.visibility = View.VISIBLE
        }
    }
}
