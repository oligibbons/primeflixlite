package com.example.primeflixlite.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["channelId", "start", "end"])],
    // Optional: Delete programs if the parent Channel is deleted (Cascade)
    // For Xtream, we often map by 'relationId' (EPG ID), which might not strictly match a Channel PK.
    // So we'll keep it loose for now to prevent accidental wipes.
)
data class Programme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String, // Maps to Channel.relationId (the EPG ID)
    val playlistUrl: String, // To group data by provider
    val title: String,
    val description: String?,
    val start: Long, // Epoch millis
    val end: Long,   // Epoch millis
    val icon: String? = null
)