package ru.practice.support;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practice.support.Ticket.*;

@RestController
@RequestMapping("/api")
public class TicketController {
  private final TicketService service;
  private final TicketRepository repository;
  private final Clock clock;

  public TicketController(TicketService service, TicketRepository repository, Clock clock) {
    this.service = service;
    this.repository = repository;
    this.clock = clock;
  }

  @PostMapping("/tickets")
  ResponseEntity<Ticket> create(@Valid @RequestBody Input.Create input) {
    Ticket ticket = service.create(input);
    return ResponseEntity.created(URI.create("/api/tickets/" + ticket.id())).body(ticket);
  }

  @GetMapping("/tickets/{id}")
  Ticket get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PutMapping("/tickets/{id}")
  Ticket edit(@PathVariable UUID id, @Valid @RequestBody Input.Edit input) {
    return service.edit(id, input);
  }

  @PatchMapping("/tickets/{id}/status")
  Ticket change(@PathVariable UUID id, @Valid @RequestBody Input.Change input) {
    return service.change(id, input.status());
  }

  @GetMapping("/tickets/{id}/history")
  List<Map<String, Object>> history(@PathVariable UUID id) {
    service.get(id);
    return repository.history(id);
  }

  @GetMapping("/categories")
  List<Map<String, Object>> categories() {
    return repository.categories();
  }

  @GetMapping("/tickets")
  TicketRepository.Page list(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) Priority priority,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Boolean overdue,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return repository.search(status, priority, categoryId, overdue, page, size, clock.instant());
  }
}
