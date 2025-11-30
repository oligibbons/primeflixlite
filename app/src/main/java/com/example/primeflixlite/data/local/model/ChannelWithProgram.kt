package com.example.primeflixlite.data.local.model

import androidx.room.Embedded
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme

/**
 * A helper class for Room to return a Channel combined with its
 * currently active EPG Programme (if any).
 */
data class ChannelWithProgram(
    @Embedded
    val channel: Channel,

    @Embedded(prefix = "prog_") // Maps columns like 'prog_title' to this object
    val program: Programme?
)