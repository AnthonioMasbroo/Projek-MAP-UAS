package com.example.projectuas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.projectuas.models.Project


class AddProjectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project)

        val projectData = intent.getParcelableExtra<Project>("projectData")
        val isEditMode = intent.getBooleanExtra("isEditMode", false)

        val fragment = AddProjectFragment().apply {
            arguments = Bundle().apply {
                putParcelable("projectData", projectData)
                putBoolean("isEditMode", isEditMode)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}