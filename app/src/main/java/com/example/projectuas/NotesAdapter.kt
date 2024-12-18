package com.example.projectuas

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class NoteItem(
    val content: String,
    val isChecklist: Boolean = false,
    val isImage: Boolean = false,
    val isVideo: Boolean = false,
    val isAudio: Boolean = false,
    val isFile: Boolean = false,
    val uri: Uri? = null,
    val checklistItems: List<ChecklistItem>? = null
)

data class ChecklistItem(
    val description: String,
    var isChecked: Boolean = false
)

class NotesAdapter(private val notes: List<NoteItem>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val videoView: ImageView = itemView.findViewById(R.id.videoView)
        val audioView: ImageView = itemView.findViewById(R.id.audioView)
        val fileView: ImageView = itemView.findViewById(R.id.fileView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.textView.text = note.content
        if (note.isChecklist) {
            holder.textView.visibility = View.GONE
            holder.checkBox.visibility = View.VISIBLE
            holder.checkBox.text = note.content
        } else {
            holder.textView.visibility = View.VISIBLE
            holder.checkBox.visibility = View.GONE
            holder.textView.text = note.content
        }

        if (note.isImage && note.uri != null) {
            holder.imageView.visibility = View.VISIBLE
            holder.imageView.setImageURI(note.uri)
        } else {
            holder.imageView.visibility = View.GONE
        }

        if (note.isVideo && note.uri != null) {
            holder.videoView.visibility = View.VISIBLE
            holder.videoView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, note.uri)
                intent.setDataAndType(note.uri, "video/*")
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.videoView.visibility = View.GONE
        }

        if (note.isAudio && note.uri != null) {
            holder.audioView.visibility = View.VISIBLE
            holder.audioView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, note.uri)
                intent.setDataAndType(note.uri, "audio/*")
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.audioView.visibility = View.GONE
        }

        if (note.isFile && note.uri != null) {
            holder.fileView.visibility = View.VISIBLE
            holder.fileView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, note.uri)
                intent.setDataAndType(note.uri, "*/*")
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.fileView.visibility = View.GONE
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (notes[position].isChecklist) TYPE_CHECKLIST else TYPE_NOTE
    }



    override fun getItemCount(): Int = notes.size

    companion object {
        private const val TYPE_NOTE = 1
        private const val TYPE_CHECKLIST = 2
    }
}