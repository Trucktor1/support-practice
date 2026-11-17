# Тест-план
| Требования | Проверки | Уровень |
|---|---|---|
| US1, US7 | blankTitleInvalid, missingCategoryInvalid, validation400, migrationAndCategories | Unit / PostgreSQL |
| US2 | unknownTicket404, unknown404, creationAndHistory | Unit / PostgreSQL |
| US3–US5 | transitions, forbiddenTransitionDoesNotWrite, lifecycle, forbiddenOperationKeepsData | Unit / E2E |
| US6 | filterAndPage | PostgreSQL |
| Просрочка | exactDeadlineIsNotOverdue, expiredOpenIsOverdue, closedIsNotOverdue, absentDeadlineIsNotOverdue, javaDeadlineBoundary | Java/Kotlin Unit |
| US8 | emptySelection, invalidPeriod, sameSelectionAndResolvedAverage, noResolvedReturnsNull, reportHalfOpenInterval | Kotlin / PostgreSQL |
Время внедряется через Clock. В интеграционных тестах фиксировано 2026-01-01T00:00:00Z. Перед каждой проверкой прикладные таблицы очищаются. PostgreSQL запускается Testcontainers, миграции применяются Spring Boot.
