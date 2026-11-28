package com.slm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val relation: String = "Self",
    val age: Int? = null,
    val avatarColorHex: String = "#0A6C74",
    val notes: String = "",
    val isDefault: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
