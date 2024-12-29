package com.example.projectuas.models

import android.os.Parcel
import android.os.Parcelable

data class Project(
    val projectTitle: String,
    val projectDetail: String,
    val dueDate: String,
    val taskList: List<String>,
    val memberList: List<String>,
    val userId: String,
    val documentId: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: listOf(),
        parcel.createStringArrayList() ?: listOf(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(projectTitle)
        parcel.writeString(projectDetail)
        parcel.writeString(dueDate)
        parcel.writeStringList(taskList)
        parcel.writeStringList(memberList)
        parcel.writeString(userId)
        parcel.writeString(documentId)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Project> {
        override fun createFromParcel(parcel: Parcel): Project {
            return Project(parcel)
        }

        override fun newArray(size: Int): Array<Project?> {
            return arrayOfNulls(size)
        }
    }
}