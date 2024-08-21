package com.aiocare.poc.superCat

object RecordingTypeHelper {

    fun findTypeBasedOnNames(list: List<String>): RecordingType {
        return when{
            list.any { it.contains("ISO26782") } -> RecordingType.ISO_C1C11
            list.any { it.contains("ISO23747") } -> RecordingType.ISO_PEF
            else -> RecordingType.CUSTOM_SEQUENCE
        }
    }
}