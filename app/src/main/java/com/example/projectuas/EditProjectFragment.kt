package com.example.projectuas

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.example.projectuas.models.Project


class EditProjectFragment : AddProjectFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEditMode = arguments?.getBoolean("isEditMode", false) ?: false
        if (!isEditMode) {
            Log.e("EditProjectFragment", "Fragment loaded without edit mode")
            navigateBack()
            return
        }

        arguments?.getParcelable<Project>("projectData")?.let { project ->
            currentProject = project
            fillProjectFields(project)
            view.findViewById<Button>(R.id.btnAddProject).text = "Update Project"
            Log.d("EditProjectFragment", "Edit mode initialized with project: $project")
        } ?: run {
            Log.e("EditProjectFragment", "No project data found")
            navigateBack()
        }
    }

    private fun navigateBack() {
        requireActivity().supportFragmentManager.popBackStack()
    }
}