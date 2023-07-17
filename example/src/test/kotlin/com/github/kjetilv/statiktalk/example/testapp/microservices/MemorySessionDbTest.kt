package com.github.kjetilv.statiktalk.example.testapp.microservices

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MemorySessionDbTest {

    val sessions = MemorySessionDb()

    @Test
    fun `user login state`() {

        assertTrue(userList().isEmpty())

        sessions.loggedIn(User("foo", "123", metadata = mapOf("foo" to "bar")))
        assertFalse(userList().isEmpty())

        assertUser {
            assertEquals("foo", it.userId)
            assertEquals(mapOf("foo" to "bar"), it.metadata)
            assertNull(it.status)
        }

        sessions.userChange(User("foo", "123", status = "elite"))
        assertFalse(userList().isEmpty())

        assertUser {
            assertEquals("foo", it.userId)
            assertEquals(mapOf("foo" to "bar"), it.metadata)
            assertEquals("elite", it.status)
        }
    }

    @Test
    fun `user login state, disordered`() {
        sessions.userChange(User("foo", "123", status = "elite"))
        sessions.userChange(User("foo", "123", metadata = mapOf("foo" to "bar")))

        assertTrue(sessions.sessions().isEmpty())

        sessions.loggedIn(User("foo", "123"))
        assertFalse(userList().isEmpty())

        assertUser {
            assertEquals("foo", it.userId)
            assertEquals("elite", it.status)
            assertNull(it.returning)
        }

        sessions.userChange(User("foo", "123", returning = true))
        assertUser {
            assertEquals("foo", it.userId)
            assertEquals("elite", it.status)
            assertTrue(it.returning!!)
        }

        assertUser {
            assertEquals("foo", it.userId)
            assertEquals("elite", it.status)
            assertTrue(it.returning!!)
            assertEquals(mapOf("foo" to "bar"), it.metadata)
        }
    }

    private fun assertUser(block: (User) -> Unit) = userList().first().let(block)

    private fun userList() = sessions.sessions()
}
