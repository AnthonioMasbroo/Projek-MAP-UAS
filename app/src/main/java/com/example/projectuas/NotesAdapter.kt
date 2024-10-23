package com.example.projectuas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class NoteItem(val content: String, val isChecklist: Boolean)

class NotesAdapter(private val notes: List<NoteItem>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        if (note.isChecklist) {
            holder.tvNoteContent.visibility = View.GONE
            holder.checkBox.visibility = View.VISIBLE
            holder.checkBox.text = note.content
        } else {
            holder.tvNoteContent.visibility = View.VISIBLE
            holder.checkBox.visibility = View.GONE
            holder.tvNoteContent.text = note.content
        }
    }

    override fun getItemCount(): Int = notes.size

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNoteContent: TextView = itemView.findViewById(R.id.tvNoteContent)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }
}