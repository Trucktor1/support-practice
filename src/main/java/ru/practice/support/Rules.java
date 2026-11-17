package ru.practice.support;

import ru.practice.support.Ticket.Status;

public final class Rules {
  private Rules() {}

  public static boolean editable(Status status) {
    return status == Status.NEW || status == Status.IN_PROGRESS;
  }

  public static boolean allowed(Status from, Status to) {
    return from == Status.NEW && (to == Status.IN_PROGRESS || to == Status.CANCELLED)
        || from == Status.IN_PROGRESS && (to == Status.RESOLVED || to == Status.CANCELLED);
  }
}
