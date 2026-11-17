package ru.practice.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.validation.Validation;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.practice.support.Ticket.*;

class RulesTest {
  static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @ParameterizedTest
  @CsvSource({
    "NEW,IN_PROGRESS,true",
    "NEW,CANCELLED,true",
    "IN_PROGRESS,RESOLVED,true",
    "IN_PROGRESS,CANCELLED,true",
    "NEW,RESOLVED,false",
    "RESOLVED,IN_PROGRESS,false",
    "CANCELLED,NEW,false",
    "NEW,NEW,false"
  })
  void transitions(Status from, Status to, boolean expected) {
    assertEquals(expected, Rules.allowed(from, to));
  }

  Ticket ticket(Status status, Instant due) {
    return new Ticket(UUID.randomUUID(), "Test", "", 1, Priority.LOW, status, NOW, due, null);
  }

  @Test
  void exactDeadlineIsNotOverdue() {
    assertFalse(ticket(Status.NEW, NOW).overdue(NOW));
  }

  @Test
  void expiredOpenIsOverdue() {
    assertTrue(ticket(Status.IN_PROGRESS, NOW.minusSeconds(1)).overdue(NOW));
  }

  @Test
  void closedIsNotOverdue() {
    assertFalse(ticket(Status.RESOLVED, NOW.minusSeconds(1)).overdue(NOW));
  }

  @Test
  void absentDeadlineIsNotOverdue() {
    assertFalse(ticket(Status.NEW, null).overdue(NOW));
  }

  @Test
  void blankTitleInvalid() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertFalse(
          factory
              .getValidator()
              .validate(new Input.Create(" ", "", 1L, Priority.LOW, null))
              .isEmpty());
    }
  }

  @Test
  void missingCategoryInvalid() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertFalse(
          factory
              .getValidator()
              .validate(new Input.Create("Title", "", null, Priority.LOW, null))
              .isEmpty());
    }
  }

  @Test
  void unknownTicket404() {
    var repository = mock(TicketRepository.class);
    UUID id = UUID.randomUUID();
    when(repository.find(id, false)).thenReturn(Optional.empty());
    assertEquals(
        404,
        assertThrows(
                ApiFailure.class,
                () -> new TicketService(repository, Clock.fixed(NOW, ZoneOffset.UTC)).get(id))
            .status());
  }

  @Test
  void forbiddenTransitionDoesNotWrite() {
    var repository = mock(TicketRepository.class);
    Ticket ticket = ticket(Status.RESOLVED, NOW);
    when(repository.find(ticket.id(), true)).thenReturn(Optional.of(ticket));
    assertEquals(
        409,
        assertThrows(
                ApiFailure.class,
                () ->
                    new TicketService(repository, Clock.fixed(NOW, ZoneOffset.UTC))
                        .change(ticket.id(), Status.NEW))
            .status());
    verify(repository, never()).status(any(), any(), any());
    verify(repository, never()).history(any(), any(), any(), any());
  }
}
