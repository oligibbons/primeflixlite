package com.example.primeflixlite.data.local.model

import androidx.room.Embedded
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.Programme

data class ChannelWithProgram(
    @Embedded val channel: Channel,
    @Embedded(prefix = "prog_") val program: Programme?
)