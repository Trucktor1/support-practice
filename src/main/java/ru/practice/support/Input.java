package ru.practice.support;

import jakarta.validation.constraints.*;
import java.time.Instant;
import ru.practice.support.Ticket.*;

public final class Input {
  private Input() {}

  public record Create(
      @NotBlank @Size(max = 200) String title,
      @NotNull @Size(max = 20000) String description,
      @NotNull @Positive Long categoryId,
      @NotNull Priority priority,
      Instant dueAt) {}

  public record Edit(
      @NotNull @Size(max = 20000) String description, @NotNull Priority priority, Instant dueAt) {}

  public record Change(@NotNull Status status) {}
}
