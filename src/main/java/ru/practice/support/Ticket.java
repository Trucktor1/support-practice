package ru.practice.support;

import java.time.Instant;
import java.util.UUID;

public record Ticket(
    UUID id,
    String title,
    String description,
    long categoryId,
    Priority priority,
    Status status,
    Instant createdAt,
    Instant dueAt,
    Instant closedAt) {
  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }

  public enum Status {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED
  }

  public boolean overdue(Instant now) {
    return Rules.editable(status) && dueAt != null && dueAt.isBefore(now);
  }
}
