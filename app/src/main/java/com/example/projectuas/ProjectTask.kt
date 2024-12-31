package com.example.projectuas.models

data class ProjectTask(
    val projectId: String,
    val projectName: String,
    val teamMembers: List<String>,
    val progress: String,
    val projectImage1: Int // Resource ID untuk gambar profil 1
)
