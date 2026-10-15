package com.example.passerwatch.monitor

import org.junit.Assert.*
import org.junit.Test

class SessionTrackerTest {
    @Test fun recreatedTrackerCannotReuseApplicationSaveSession() {
        val old = SessionTracker().start()
        val recreated = SessionTracker()
        val current = recreated.start()
        assertNotEquals(old, current)
        assertFalse(recreated.accepts(old))
    }
    @Test fun onlyCurrentRunningSessionIsAccepted() {
        val sessions = SessionTracker()
        val first = sessions.start()
        assertTrue(sessions.accepts(first))
        sessions.stop()
        assertFalse(sessions.accepts(first))
        val next = sessions.start()
        assertNotEquals(first, next)
        assertFalse(sessions.accepts(first))
        assertTrue(sessions.accepts(next))
    }
}
