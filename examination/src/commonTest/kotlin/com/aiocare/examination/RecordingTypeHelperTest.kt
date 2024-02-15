package com.aiocare.examination

import com.aiocare.poc.superCat.RecordingType
import com.aiocare.poc.superCat.RecordingTypeHelper
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingTypeHelperTest {

    @Test
    fun test(){
        assertEquals(RecordingType.CUSTOM_SEQUENCE, RecordingTypeHelper.findTypeBasedOnNames(listOf("22", "333")))
        assertEquals(RecordingType.ISO_C1C11, RecordingTypeHelper.findTypeBasedOnNames(listOf(
            "C1-C13 (ISO26782)/eee",
            "333"
        )))
        assertEquals(RecordingType.ISO_PEF, RecordingTypeHelper.findTypeBasedOnNames(listOf(
            "C1-C13 (IxSO26782)/eee",
            "PEF (ISO23747)"
        )))

    }
}