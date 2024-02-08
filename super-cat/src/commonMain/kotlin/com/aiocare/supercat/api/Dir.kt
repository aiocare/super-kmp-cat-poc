package com.aiocare.supercat.api

import kotlinx.serialization.Serializable

@Serializable
data class Dir(val name: String, val files: List<WaveFormFile>, val dirs: List<Dir>)
