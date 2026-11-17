package ru.practice.support;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class Errors {
  private final Clock clock;

  public Errors(Clock clock) {
    this.clock = clock;
  }

  public record ErrorBody(Instant timestamp, int status, String message, String path) {}

  @ExceptionHandler(ApiFailure.class)
  ResponseEntity<ErrorBody> business(ApiFailure e, HttpServletRequest r) {
    return response(e.status(), e.getMessage(), r);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<ErrorBody> invalid(Exception e, HttpServletRequest r) {
    return response(400, "Invalid request", r);
  }

  private ResponseEntity<ErrorBody> response(int status, String message, HttpServletRequest r) {
    return ResponseEntity.status(status)
        .body(new ErrorBody(clock.instant(), status, message, r.getRequestURI()));
  }
}
