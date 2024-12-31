package com.example.projectuas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectuas.R
import com.example.projectuas.models.PrivateTask

class ArchiveAdapter(
    private val archivedTasks: List<PrivateTask>,
    private val listener: OnDeleteClickListener,
    private val restoreListener: OnRestoreClickListener

) : RecyclerView.Adapter<ArchiveAdapter.ArchiveTaskViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    inner class ArchiveTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTaskName: TextView = itemView.findViewById(R.id.tvArchivedTaskName)
        val imgDelete: ImageView = itemView.findViewById(R.id.imgDelete)
        val imgRestore: ImageView = itemView.findViewById(R.id.imgRestore)
        val tvTaskProgress: TextView = itemView.findViewById(R.id.tvArchivedTaskProgress)
    }

    interface OnRestoreClickListener {
        fun onRestoreClick(archiveId: String, position: Int)
    }


    interface OnDeleteClickListener {
        fun onDeleteClick(archiveId: String, position: Int)
    }

    interface OnItemClickListener {
        fun onItemClick(projectId: String, taskName: String) // Ubah parameter
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archive, parent, false)
        return ArchiveTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchiveTaskViewHolder, position: Int) {
        val archivedTask = archivedTasks[position]
        holder.tvTaskName.text = archivedTask.taskName
        holder.tvTaskProgress.text = archivedTask.progress

        holder.imgDelete.setOnClickListener {
            listener.onDeleteClick(archivedTask.projectId, holder.adapterPosition)
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(archivedTask.projectId, archivedTask.taskName)
        }

        holder.imgRestore.setOnClickListener {
            restoreListener.onRestoreClick(archivedTask.projectId, holder.adapterPosition)
        }

    }

    override fun getItemCount(): Int = archivedTasks.size
}