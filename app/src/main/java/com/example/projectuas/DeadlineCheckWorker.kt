package com.example.projectuas

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DeadlineCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun doWork(): Result {
        val currentUser = auth.currentUser ?: return Result.failure()
        val sharedPref = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val isNotificationEnabled = sharedPref.getBoolean("notifications_enabled", false)

        if (!isNotificationEnabled) {
            return Result.success()
        }

        checkDeadlines(currentUser)
        return Result.success()
    }

    private fun checkDeadlines(currentUser: FirebaseUser) {
        val username = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            .getString("username", "User") ?: "User"

        // Cek Private Projects
        checkPrivateProjects(currentUser, username)
        // Cek Group Projects
        checkGroupProjects(currentUser, username)
    }

    private fun checkPrivateProjects(currentUser: FirebaseUser, username: String) {
        firestore.collection("projects")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("memberList", emptyList<String>())
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val dueDate = doc.getString("dueDate")?.let { parseDate(it) } ?: continue
                    val projectTitle = doc.getString("projectTitle") ?: continue

                    val remainingTime = calculateRemainingTime(dueDate)
                    if (remainingTime.isNotEmpty()) {
                        NotificationHelper.showPrivateProjectNotification(
                            context,
                            username,
                            projectTitle,
                            remainingTime
                        )
                    }
                }
            }
    }

    private fun checkGroupProjects(currentUser: FirebaseUser, username: String) {
        firestore.collection("projects")
            .whereArrayContains("memberList", "${currentUser.email} ($username)")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val dueDate = doc.getString("dueDate")?.let { parseDate(it) } ?: continue
                    val projectTitle = doc.getString("projectTitle") ?: continue

                    val remainingTime = calculateRemainingTime(dueDate)
                    if (remainingTime.isNotEmpty()) {
                        NotificationHelper.showGroupProjectNotification(
                            context,
                            username,
                            projectTitle,
                            remainingTime
                        )
                    }
                }
            }
    }

    private fun parseDate(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateRemainingTime(dueDate: Date): String {
        val now = Date()
        val diffInMillis = dueDate.time - now.time

        if (diffInMillis <= 0) return ""

        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60

        return when {
            days > 0 -> "$days hari, $hours jam"
            hours > 0 -> "$hours jam, $minutes menit"
            else -> "$minutes menit"
        }
    }
}