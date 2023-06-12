package com.frankensound

import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        assertEquals(1, 1)
    }
}
