package com.example.projectuas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import coil.load
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NoteItem(
    val content: String,
    val isChecklist: Boolean = false,
    val isImage: Boolean = false,
    val isVideo: Boolean = false,
    val isAudio: Boolean = false,
    val isFile: Boolean = false,
    val uri: Uri? = null,
    val fileName: String? = null,
    val audioUrl: Uri? = null,
    val videoThumbnailUri: String? = null,
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
        val videoFrame: FrameLayout = itemView.findViewById(R.id.videoFrame)
        val videoView: ImageView = itemView.findViewById(R.id.videoView)
        val playIcon: ImageView = itemView.findViewById(R.id.playIcon)
        val fileView: ImageView = itemView.findViewById(R.id.fileView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val checklistContainer: LinearLayout = itemView.findViewById(R.id.checklistContainer)
        val filePreviewLayout: LinearLayout = itemView.findViewById(R.id.filePreviewLayout)
        val fileName: TextView = itemView.findViewById(R.id.fileName)
        val audioPlaybackLayout: LinearLayout = itemView.findViewById(R.id.audioPlaybackLayout)
        val btnPlayPause: ImageButton = itemView.findViewById(R.id.btnPlayPause)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val progressBarWaveform: ProgressBar = itemView.findViewById(R.id.progressBarWaveform)
        val videoPlaybackView: VideoView = itemView.findViewById(R.id.videoPlaybackView)
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = -1
    private val handler = Handler(Looper.getMainLooper())

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

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
        holder.videoFrame.visibility = View.GONE
        holder.videoView.visibility = View.GONE
        holder.playIcon.visibility = View.GONE
        holder.fileView.visibility = View.GONE
        holder.audioPlaybackLayout.visibility = View.GONE

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
            holder.imageView.load(note.uri) {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(R.drawable.error_placeholder)
            }

            holder.imageView.setOnClickListener {
                showImagePreview(note.uri, holder.itemView.context)
            }
        }

        // Menampilkan Video jika ada
        if (note.isVideo && note.uri != null) {
            holder.videoFrame.visibility = View.VISIBLE
            holder.videoView.visibility = View.VISIBLE
            holder.playIcon.visibility = View.VISIBLE

            // Load thumbnail via Coil
            if (!note.videoThumbnailUri.isNullOrEmpty()) {
                holder.videoView.load(note.videoThumbnailUri) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.error_placeholder)
                }
            } else {
                // Fallback ke generate thumbnail jika tidak ada thumbnailUri
                coroutineScope.launch {
                    val thumbnail = generateVideoThumbnail(holder.itemView.context, note.uri)
                    if (thumbnail != null) {
                        holder.videoView.setImageBitmap(thumbnail)
                    } else {
                        holder.videoView.setImageResource(R.drawable.error_placeholder)
                    }
                }
            }

            holder.videoFrame.setOnClickListener {
                showVideoOptionsDialog(holder.itemView.context, note.uri)
            }
        }

        // Menampilkan Audio jika ada
        if (note.isAudio && note.audioUrl != null) {
            holder.audioPlaybackLayout.visibility = View.VISIBLE
            holder.btnPlayPause.setImageResource(R.drawable.ic_play)
            holder.tvDuration.text = "00:00"

            // Setup MediaPlayer
            holder.btnPlayPause.setOnClickListener {
                if (currentPlayingPosition == position) {
                    if (mediaPlayer?.isPlaying == true) {
                        pauseAudio()
                        holder.btnPlayPause.setImageResource(R.drawable.ic_play)
                    } else {
                        resumeAudio()
                        holder.btnPlayPause.setImageResource(R.drawable.ic_pause)
                    }
                } else {
                    stopAudio()
                    playAudio(holder, position, note.audioUrl!!)
                }
            }
        } else {
            holder.audioPlaybackLayout.visibility = View.GONE
        }

        // Menampilkan File jika ada
        if (note.isFile && note.uri != null) {
            holder.filePreviewLayout.visibility = View.VISIBLE

            // Gunakan nama file dari Firestore
            val fileName = note.fileName ?: "Unknown file"
            holder.fileName.text = fileName

            holder.filePreviewLayout.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(note.uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        holder.itemView.context,
                        "No app found to open this file",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } else {
            holder.filePreviewLayout.visibility = View.GONE
        }

    }

    // Fungsi suspend untuk menghasilkan thumbnail video
    private suspend fun generateVideoThumbnail(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val bitmap = retriever.frameAtTime
                retriever.release()
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Fungsi untuk menampilkan dialog pemutaran video
    private fun showVideoDialog(context: Context, uri: Uri) {
        val dialogBuilder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_video_preview, null)
        val videoPreview: VideoView = dialogView.findViewById(R.id.videoView)

        videoPreview.setVideoURI(uri)
        videoPreview.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = false
            videoPreview.start()
        }

        videoPreview.setOnCompletionListener {
            videoPreview.stopPlayback()
        }

        dialogBuilder.setView(dialogView)
            .setPositiveButton("Tutup") { dialog, _ ->
                videoPreview.stopPlayback()
                dialog.dismiss()
            }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    // Fungsi untuk menampilkan dialog opsi pemutaran video
    private fun showVideoOptionsDialog(context: Context, uri: Uri) {
        val options = arrayOf("Play in App", "Open with External App", "Cancel")
        AlertDialog.Builder(context)
            .setTitle("Select Video Playback")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Play in App
                        val intent = Intent(context, VideoPlaybackActivity::class.java).apply {
                            putExtra("videoUri", uri.toString())
                        }
                        context.startActivity(intent)
                    }
                    1 -> { // Open with External App
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Choose App to Play Video"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to play video", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> { // Cancel
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    // Fungsi untuk menampilkan preview gambar dalam dialog
    private fun showImagePreview(uri: Uri, context: Context) {
        val dialogBuilder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_image_preview, null)
        val imageView: ImageView = dialogView.findViewById(R.id.imagePreview)

        imageView.load(uri) {
            crossfade(true)
            placeholder(R.drawable.placeholder)
            error(R.drawable.error_placeholder)
        }

        dialogBuilder.setView(dialogView)
            .setPositiveButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    override fun getItemCount(): Int = notes.size

    // Fungsi untuk memainkan audio
    private fun playAudio(holder: NoteViewHolder, position: Int, uri: Uri) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(holder.itemView.context, uri)
                prepare()
                start()
                holder.btnPlayPause.setImageResource(R.drawable.ic_pause)
                holder.audioPlaybackLayout.visibility = View.VISIBLE
                holder.progressBarWaveform.visibility = View.VISIBLE
                currentPlayingPosition = position

                // Update durasi
                val duration = this.duration
                holder.tvDuration.text = formatDuration(duration)

                // Update ProgressBar
                holder.progressBarWaveform.max = duration
                updateProgress(holder)

                // Callback ketika selesai
                setOnCompletionListener {
                    holder.btnPlayPause.setImageResource(R.drawable.ic_play)
                    holder.progressBarWaveform.visibility = View.GONE
                    currentPlayingPosition = -1
                    holder.progressBarWaveform.progress = 0
                }

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(holder.itemView.context, "Gagal memutar audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fungsi untuk mengupdate progress audio
    private fun updateProgress(holder: NoteViewHolder) {
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        holder.progressBarWaveform.progress = it.currentPosition
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        })
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
    }

    private fun resumeAudio() {
        mediaPlayer?.start()
    }

    private fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentPlayingPosition = -1
    }

    // Fungsi untuk mengubah durasi audio menjadi format menit:detik
    private fun formatDuration(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onViewRecycled(holder: NoteViewHolder) {
        super.onViewRecycled(holder)
        if (currentPlayingPosition == holder.adapterPosition) {
            // Hentikan pemutaran audio
            stopAudio()

            // Hentikan pemutaran video
            holder.videoPlaybackView.stopPlayback()
            holder.videoPlaybackView.visibility = View.GONE
            holder.videoView.visibility = View.VISIBLE

            // Reset posisi pemutaran
            currentPlayingPosition = -1
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        coroutineScope.cancel() // Membatalkan semua coroutine saat adapter tidak digunakan
    }
}
