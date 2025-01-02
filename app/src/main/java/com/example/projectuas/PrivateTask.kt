package com.example.projectuas.models

import java.io.Serializable

data class PrivateTask(
    val projectId: String,
    val taskName: String,
    val progress: String,
    var documentId: String = ""
) : Serializable
