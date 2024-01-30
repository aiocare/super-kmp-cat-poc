package com.aiocare.poc.superCat.custom

data class Dir(val name: String, val files: List<WaveFormFile>, val dirs: List<Dir> )