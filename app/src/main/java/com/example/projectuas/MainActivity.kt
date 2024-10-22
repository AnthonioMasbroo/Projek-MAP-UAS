package com.example.projectuas

import com.google.firebase.FirebaseApp
import android.os.Bundle
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

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_create -> {
                    replaceFragment(CreateFragment())
                    true
                }
                R.id.nav_archive -> {
                    replaceFragment(ArchiveFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi ini dipanggil ketika login sukses dan menerima username
    fun onLoginSuccess(username: String) {
        // Buat bundle dan masukkan username
        val bundle = Bundle()
        bundle.putString("username", username)

        // Buat HomeFragment dan set argumen bundle
        val homeFragment = HomeFragment()
        homeFragment.arguments = bundle

        // Ganti fragment ke HomeFragment dan tampilkan BottomNavigationView
        replaceFragment(homeFragment)
        bottomNavigationView.visibility = BottomNavigationView.VISIBLE
    }

    // Fungsi untuk mengganti fragment
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
