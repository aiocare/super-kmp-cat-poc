package com.aiocare.examination

import com.aiocare.supercat.NameHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NameHelperTest {

    @Test
    fun parse(){
        assertEquals("aaa", NameHelper.parse("aaa"))
        assertEquals("aaa", NameHelper.parse("bb@aaa"))
        assertEquals("c", NameHelper.parse("bb@aaa@c"))
        assertNotEquals("aaa", NameHelper.parse("bb@aaa@c"))
    }
}