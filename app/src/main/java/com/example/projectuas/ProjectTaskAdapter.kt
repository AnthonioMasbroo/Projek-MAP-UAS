package com.example.projectuas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectuas.R
import com.example.projectuas.models.ProjectTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProjectTaskAdapter(
    private val projectTasks: MutableList<ProjectTask>,
    private val listener: OnProjectDeleteClickListener
) : RecyclerView.Adapter<ProjectTaskAdapter.ProjectTaskViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    interface OnProjectDeleteClickListener {
        fun onProjectDeleteClick(position: Int)
    }

    interface OnProjectClickListener {
        fun onProjectClick(projectId: String)
    }

    // Fungsi untuk set click listener
    fun setOnProjectClickListener(listener: OnProjectClickListener) {
        onProjectClickListener = listener
    }

    private var onProjectClickListener: OnProjectClickListener? = null

    inner class ProjectTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProjectName: TextView = itemView.findViewById(R.id.tvProjectName)
        val imgProfile1: ImageView = itemView.findViewById(R.id.imgProfile1)
        val imgProfile2: ImageView = itemView.findViewById(R.id.imgProfile2)
        val tvTeamMembers: TextView = itemView.findViewById(R.id.tvTeamMembers)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        val imgDelete: ImageView = itemView.findViewById(R.id.imgDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_task, parent, false)
        return ProjectTaskViewHolder(view)
    }

    private fun setDeleteButtonVisibility(holder: ProjectTaskViewHolder, projectId: String) {
        firestore.collection("projects").document(projectId).get()
            .addOnSuccessListener { doc ->
                val adminId = doc.getString("adminId")
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                holder.imgDelete.visibility = if (adminId == currentUserId) View.VISIBLE else View.GONE
            }
    }

    override fun onBindViewHolder(holder: ProjectTaskViewHolder, position: Int) {
        val projectTask = projectTasks[position]
        holder.tvProjectName.text = projectTask.projectName
        holder.imgProfile1.setImageResource(projectTask.projectImage1)
        holder.imgProfile2.setImageResource(projectTask.projectImage2)
        holder.tvTeamMembers.text = "Team members (${projectTask.teamMembers.size})"
        holder.tvProgress.text = projectTask.progress
        holder.imgDelete.visibility = View.GONE // Default hidden
        setDeleteButtonVisibility(holder, projectTask.projectId)

        // Click listener untuk delete
        holder.imgDelete.setOnClickListener {
            listener.onProjectDeleteClick(holder.adapterPosition)
        }

        // Click listener untuk seluruh item
        holder.itemView.setOnClickListener {
            onProjectClickListener?.onProjectClick(projectTask.projectId)
        }
    }

    override fun getItemCount(): Int = projectTasks.size
}
