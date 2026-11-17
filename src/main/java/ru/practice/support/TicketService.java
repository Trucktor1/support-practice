package ru.practice.support;

import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practice.support.Ticket.*;

@Service
public class TicketService {
  private final TicketRepository repository;
  private final Clock clock;

  public TicketService(TicketRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public Ticket get(UUID id) {
    return required(id, false);
  }

  private Ticket required(UUID id, boolean lock) {
    return repository.find(id, lock).orElseThrow(() -> new ApiFailure(404, "Ticket not found"));
  }

  @Transactional
  public Ticket create(Input.Create input) {
    if (!repository.category(input.categoryId())) throw new ApiFailure(400, "Unknown category");
    Ticket ticket =
        new Ticket(
            UUID.randomUUID(),
            input.title(),
            input.description(),
            input.categoryId(),
            input.priority(),
            Status.NEW,
            clock.instant(),
            input.dueAt(),
            null);
    repository.insert(ticket);
    repository.history(ticket.id(), null, Status.NEW, ticket.createdAt());
    return ticket;
  }

  @Transactional
  public Ticket edit(UUID id, Input.Edit input) {
    if (!Rules.editable(required(id, true).status())) throw new ApiFailure(409, "Ticket is closed");
    repository.edit(id, input);
    return get(id);
  }

  @Transactional
  public Ticket change(UUID id, Status next) {
    Ticket ticket = required(id, true);
    if (!Rules.allowed(ticket.status(), next)) throw new ApiFailure(409, "Forbidden transition");
    Instant now = clock.instant();
    repository.status(id, next, Rules.editable(next) ? null : now);
    repository.history(id, ticket.status(), next, now);
    return get(id);
  }
}
