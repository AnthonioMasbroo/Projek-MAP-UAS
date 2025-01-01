package com.example.projectuas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectuas.R
import com.example.projectuas.models.ProjectTask

class ProjectTaskAdapter(
    private val projectTasks: MutableList<ProjectTask>,
    private val doneListener: OnProjectDoneClickListener,
    private val archiveListener: OnProjectArchiveClickListener,
    private val isArchiveList: Boolean = false // Tambahkan parameter untuk mendukung daftar arsip
) : RecyclerView.Adapter<ProjectTaskAdapter.ProjectTaskViewHolder>() {

    interface OnProjectDoneClickListener {
        fun onProjectDoneClick(position: Int)
    }

    interface OnProjectArchiveClickListener {
        fun onProjectArchiveClick(position: Int)
    }

    interface OnProjectClickListener {
        fun onProjectClick(projectId: String)
    }

    fun setOnProjectClickListener(listener: OnProjectClickListener) {
        onProjectClickListener = listener
    }

    private var onProjectClickListener: OnProjectClickListener? = null

    inner class ProjectTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProjectName: TextView = itemView.findViewById(R.id.tvProjectName)
        val imgProfile: ImageView = itemView.findViewById(R.id.imgProfile)
        val tvTeamMembers: TextView = itemView.findViewById(R.id.tvTeamMembers)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        val imgDoneProject: ImageView = itemView.findViewById(R.id.imgDoneProject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_task, parent, false)
        return ProjectTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectTaskViewHolder, position: Int) {
        val projectTask = projectTasks[position]
        holder.tvProjectName.text = projectTask.projectName
        holder.imgProfile.setImageResource(projectTask.projectImage1)
        holder.tvTeamMembers.text = "Team members (${projectTask.teamMembers.size})"
        holder.tvProgress.text = projectTask.progress

        if (isArchiveList) {
            // Untuk daftar arsip: sembunyikan tombol done
            holder.imgDoneProject.visibility = View.GONE
        } else {
            // Untuk daftar biasa: tombol checklist berfungsi sebagai archive
            holder.imgDoneProject.visibility = View.VISIBLE
            holder.imgDoneProject.setOnClickListener {
                archiveListener.onProjectArchiveClick(holder.adapterPosition)
            }
        }

        holder.itemView.setOnClickListener {
            onProjectClickListener?.onProjectClick(projectTask.projectId)
        }
    }

    override fun getItemCount(): Int = projectTasks.size
}