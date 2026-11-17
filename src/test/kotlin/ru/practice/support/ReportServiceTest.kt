package ru.practice.support

import java.time.*
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReportServiceTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun emptySelection() {
        val report =
            ReportService(ReportSource { _, _ -> emptyList() }, clock)
                .summary(now.minusSeconds(1), now)
        assertEquals(0, report.createdCount)
        assertNull(report.averageResolutionSeconds)
        assertEquals(4, report.byStatus.size)
    }

    @Test
    fun invalidPeriod() {
        assertEquals(
            400,
            assertThrows(ApiFailure::class.java) {
                    ReportService(ReportSource { _, _ -> emptyList() }, clock).summary(now, now)
                }
                .status(),
        )
    }

    @Test
    fun javaDeadlineBoundary() {
        val ticket =
            Ticket(
                UUID.randomUUID(),
                "Test",
                "",
                1,
                Ticket.Priority.HIGH,
                Ticket.Status.NEW,
                now.minusSeconds(10),
                now,
                null,
            )
        assertFalse(ticket.overdue(now))
        assertTrue(ticket.overdue(now.plusNanos(1)))
    }

    @Test
    fun sameSelectionAndResolvedAverage() {
        val tickets =
            listOf(
                Ticket(
                    UUID.randomUUID(),
                    "A",
                    "",
                    1,
                    Ticket.Priority.HIGH,
                    Ticket.Status.RESOLVED,
                    now.minusSeconds(10),
                    null,
                    now,
                ),
                Ticket(
                    UUID.randomUUID(),
                    "B",
                    "",
                    1,
                    Ticket.Priority.LOW,
                    Ticket.Status.NEW,
                    now.minusSeconds(5),
                    now.minusSeconds(1),
                    null,
                ),
                Ticket(
                    UUID.randomUUID(),
                    "C",
                    "",
                    1,
                    Ticket.Priority.LOW,
                    Ticket.Status.CANCELLED,
                    now.minusSeconds(5),
                    null,
                    now,
                ),
            )
        val report =
            ReportService(ReportSource { _, _ -> tickets }, clock)
                .summary(now.minusSeconds(20), now)
        assertEquals(3, report.createdCount)
        assertEquals(10.0, report.averageResolutionSeconds)
        assertEquals(1, report.overdueOpenCount)
        assertEquals(2, report.byPriority[Ticket.Priority.LOW])
    }

    @Test
    fun noResolvedReturnsNull() {
        val ticket =
            Ticket(
                UUID.randomUUID(),
                "A",
                "",
                1,
                Ticket.Priority.LOW,
                Ticket.Status.NEW,
                now,
                null,
                null,
            )
        assertNull(
            ReportService(ReportSource { _, _ -> listOf(ticket) }, clock)
                .summary(now.minusSeconds(1), now.plusSeconds(1))
                .averageResolutionSeconds
        )
    }
}
