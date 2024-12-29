package com.example.projectuas

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
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
    val checklistItems: List<ChecklistItem> = listOf()
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
        val checklistContainer: LinearLayout = itemView.findViewById(R.id.checklistContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        // Reset semua view
        holder.textView.visibility = View.GONE
        holder.checkBox.visibility = View.GONE
        holder.checklistContainer.visibility = View.GONE
        holder.imageView.visibility = View.GONE
        holder.videoView.visibility = View.GONE
        holder.audioView.visibility = View.GONE
        holder.fileView.visibility = View.GONE

        // Tampilkan note content jika ada
        if (note.content.isNotEmpty()) {
            holder.textView.visibility = View.VISIBLE
            holder.textView.text = note.content
        }

        // Tampilkan checklist jika ada
        if (note.isChecklist && note.checklistItems.isNotEmpty()) {
            holder.checklistContainer.visibility = View.VISIBLE
            holder.checklistContainer.removeAllViews()
            note.checklistItems.forEach { checklistItem ->
                val checkBox = CheckBox(holder.itemView.context).apply {
                    text = checklistItem.description
                    isChecked = checklistItem.isChecked
                    isEnabled = true
                }
                holder.checklistContainer.addView(checkBox)
            }
        }

        // Menampilkan Image jika ada
        if (note.isImage && note.uri != null) {
            holder.imageView.visibility = View.VISIBLE
            holder.imageView.setImageURI(note.uri)
        }

        // Menampilkan Video jika ada
        if (note.isVideo && note.uri != null) {
            holder.videoView.visibility = View.VISIBLE
            holder.videoView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, note.uri)
                intent.setDataAndType(note.uri, "video/*")
                holder.itemView.context.startActivity(intent)
            }
        }

        // Menampilkan Audio jika ada
        if (note.isAudio && note.uri != null) {
            holder.audioView.visibility = View.VISIBLE
            holder.audioView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, note.uri)
                intent.setDataAndType(note.uri, "audio/*")
                holder.itemView.context.startActivity(intent)
            }
        }

        // Menampilkan File jika ada
        if (note.isFile && note.uri != null) {
            holder.fileView.visibility = View.VISIBLE
            holder.fileView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, note.uri)
                intent.setDataAndType(note.uri, "*/*")
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = notes.size
}
