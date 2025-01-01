package com.example.projectuas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectuas.R
import com.example.projectuas.models.PrivateTask

class PrivateTaskAdapter(
    private val privateTasks: MutableList<PrivateTask>,
    private val listener: SendToArchiveClickListener
) : RecyclerView.Adapter<PrivateTaskAdapter.PrivateTaskViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    interface SendToArchiveClickListener {
        fun sendToArchiveClick(position: Int)
    }

    interface OnItemClickListener {
        fun onItemClick(projectId: String, taskName: String) // Ubah parameter
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    inner class PrivateTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        val imgDone: ImageView = itemView.findViewById(R.id.imgDone)
        val tvTaskProgress: TextView = itemView.findViewById(R.id.tvTaskProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivateTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_private_task, parent, false)
        return PrivateTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrivateTaskViewHolder, position: Int) {
        val privateTask = privateTasks[position]
        holder.tvTaskName.text = privateTask.taskName
        holder.tvTaskProgress.text = privateTask.progress

        holder.imgDone.setOnClickListener {
            listener.sendToArchiveClick(holder.adapterPosition)
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(privateTask.projectId, privateTask.taskName)
        }
    }

    override fun getItemCount(): Int = privateTasks.size
}
