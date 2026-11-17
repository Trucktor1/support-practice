# Сервис обработки заявок
Новый индивидуальный проект на Java 21 и Kotlin 2.1.21. Spring Boot 3.5.7, Gradle Wrapper 8.14, PostgreSQL 16.6, Flyway, JUnit 5, Mockito, Testcontainers 1.21.4, OpenAPI, JaCoCo.

## IntelliJ IDEA
Откройте корневую папку как Gradle-проект. Project SDK и Gradle JVM должны использовать JDK 21. Для полной проверки запустите Gradle-задачу check при работающем Docker Desktop в режиме Linux containers.

## Запуск
Скопируйте .env.example в .env и замените DB_PASSWORD собственным паролем.
В терминале корня выполните:
```sh
docker compose up -d --build --wait
python scripts/smoke.py
```
Swagger UI — http://localhost:8080/swagger-ui/index.html
OpenAPI — http://localhost:8080/v3/api-docs
Readiness — http://localhost:8080/actuator/health/readiness

Контейнеры выводят логи в stdout/stderr. Для просмотра используйте docker compose logs -f app db.
Для остановки используйте docker compose down. Том database сохраняется. Флаг -v удаляет данные.

## Тесты и покрытие
```sh
./gradlew spotlessCheck test integrationTest jacocoTestReport bootJar
```
В Windows используйте gradlew.bat. Модульные тесты запускаются задачей test. Интеграционные и сквозные тесты запускаются integrationTest с настоящим PostgreSQL в Testcontainers. Отчёты JUnit находятся в build/reports/tests, JaCoCo — build/reports/jacoco/test/html/index.html.

## Резервное копирование и восстановление
Команды ниже выполняются в Git Bash/Linux:
```sh
bash scripts/backup.sh
bash scripts/restore-test.sh backups/имя-файла.dump
bash scripts/verify-operations.sh
```
restore-test пересоздаёт отдельную БД support_restore и сверяет контрольные суммы всех прикладных таблиц. verify-operations пересоздаёт контейнеры с сохранением тома, сравнивает количество заявок и проверяет восстановление. Выполняйте проверку при отсутствии параллельной записи.