package ru.practice.support;

import java.time.Instant;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {
  private final ReportService reports;

  public ReportController(ReportService reports) {
    this.reports = reports;
  }

  @GetMapping("/api/reports/summary")
  Summary summary(@RequestParam Instant from, @RequestParam Instant to) {
    return reports.summary(from, to);
  }
}
