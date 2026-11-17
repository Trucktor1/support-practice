package ru.practice.support;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import ru.practice.support.Ticket.*;

@Repository
public class TicketRepository implements ReportSource {
  private final NamedParameterJdbcTemplate db;

  public TicketRepository(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  private static Instant instant(ResultSet r, String name) throws SQLException {
    Timestamp value = r.getTimestamp(name);
    return value == null ? null : value.toInstant();
  }

  private static final RowMapper<Ticket> MAPPER =
      (r, n) ->
          new Ticket(
              r.getObject("id", UUID.class),
              r.getString("title"),
              r.getString("description"),
              r.getLong("category_id"),
              Priority.valueOf(r.getString("priority")),
              Status.valueOf(r.getString("status")),
              instant(r, "created_at"),
              instant(r, "due_at"),
              instant(r, "closed_at"));

  public Optional<Ticket> find(UUID id, boolean lock) {
    return db
        .query(
            "SELECT * FROM tickets WHERE id=:id" + (lock ? " FOR UPDATE" : ""),
            Map.of("id", id),
            MAPPER)
        .stream()
        .findFirst();
  }

  public boolean category(long id) {
    return db.queryForObject(
            "SELECT count(*) FROM categories WHERE id=:id", Map.of("id", id), Long.class)
        > 0;
  }

  public List<Map<String, Object>> categories() {
    return db.queryForList("SELECT * FROM categories ORDER BY id", Map.of());
  }

  public void insert(Ticket t) {
    db.update(
        "INSERT INTO tickets VALUES (:id,:title,:description,:category,:priority,:status,:created,:due,:closed)",
        new MapSqlParameterSource()
            .addValue("id", t.id())
            .addValue("title", t.title())
            .addValue("description", t.description())
            .addValue("category", t.categoryId())
            .addValue("priority", t.priority().name())
            .addValue("status", t.status().name())
            .addValue("created", Timestamp.from(t.createdAt()))
            .addValue("due", timestamp(t.dueAt()))
            .addValue("closed", timestamp(t.closedAt())));
  }

  private static Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  public void edit(UUID id, Input.Edit input) {
    db.update(
        "UPDATE tickets SET description=:description,priority=:priority,due_at=:due WHERE id=:id",
        new MapSqlParameterSource("id", id)
            .addValue("description", input.description())
            .addValue("priority", input.priority().name())
            .addValue("due", timestamp(input.dueAt())));
  }

  public void status(UUID id, Status status, Instant closed) {
    db.update(
        "UPDATE tickets SET status=:status,closed_at=:closed WHERE id=:id",
        new MapSqlParameterSource("id", id)
            .addValue("status", status.name())
            .addValue("closed", timestamp(closed)));
  }

  public void history(UUID id, Status old, Status next, Instant now) {
    db.update(
        "INSERT INTO status_history(ticket_id,old_status,new_status,changed_at) VALUES (:id,:old,:next,:now)",
        new MapSqlParameterSource("id", id)
            .addValue("old", old == null ? null : old.name())
            .addValue("next", next.name())
            .addValue("now", Timestamp.from(now)));
  }

  public List<Map<String, Object>> history(UUID id) {
    return db.queryForList(
        "SELECT old_status,new_status,changed_at FROM status_history WHERE ticket_id=:id ORDER BY id",
        Map.of("id", id));
  }

  public record Page(List<Ticket> content, long total, int page, int size) {}

  public Page search(
      Status status,
      Priority priority,
      Long categoryId,
      Boolean overdue,
      int page,
      int size,
      Instant now) {
    if (page < 0 || size < 1 || size > 100) throw new ApiFailure(400, "Invalid pagination");
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    MapSqlParameterSource args = new MapSqlParameterSource();
    if (status != null) {
      where.append(" AND status=:status");
      args.addValue("status", status.name());
    }
    if (priority != null) {
      where.append(" AND priority=:priority");
      args.addValue("priority", priority.name());
    }
    if (categoryId != null) {
      where.append(" AND category_id=:category");
      args.addValue("category", categoryId);
    }
    if (overdue != null) {
      where.append(overdue ? " AND " : " AND NOT ");
      where.append("(status IN ('NEW','IN_PROGRESS') AND due_at IS NOT NULL AND due_at<:now)");
      args.addValue("now", Timestamp.from(now));
    }
    long total = db.queryForObject("SELECT count(*) FROM tickets" + where, args, Long.class);
    args.addValue("limit", size).addValue("offset", (long) page * size);
    return new Page(
        db.query(
            "SELECT * FROM tickets" + where + " ORDER BY created_at,id LIMIT :limit OFFSET :offset",
            args,
            MAPPER),
        total,
        page,
        size);
  }

  public List<Ticket> createdBetween(Instant from, Instant to) {
    return db.query(
        "SELECT * FROM tickets WHERE created_at>=:from AND created_at<:to",
        Map.of("from", Timestamp.from(from), "to", Timestamp.from(to)),
        MAPPER);
  }
}
