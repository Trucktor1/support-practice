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

## Диагностика
* База недоступна. Проверьте docker compose ps, docker compose logs db, значение DB_URL, пользователя и пароль. В Compose имя сервера db. При изменении пароля в .env уже созданная БД сохраняет прежний пароль.
* Приложение unhealthy. Проверьте docker compose logs app и /actuator/health/readiness. Убедитесь, что база готова и порт свободен. Исправьте причину и выполните docker compose up -d.
* Ошибка миграции. Изучите Flyway в логах и flyway_schema_history. Исправляйте изменение новой миграцией. Не удаляйте данные и историю миграций для сокрытия ошибки.

## GitHub и доставка
Разработка ведётся в feature/support-api. После публикации создайте PR в main.
PR запускает форматирование, тесты, покрытие, сборку контейнера, smoke и проверку сохранности/backup.
После слияния те же проверки блокируют публикацию при любой ошибке. Успешный pipeline публикует ghcr.io/OWNER/REPOSITORY:COMMIT_SHA и разворачивает образ на runner с метками self-hosted, linux, support-training.

В environment training добавьте TRAINING_DB_PASSWORD и REGISTRY_READ_TOKEN с чтением пакетов. Runner должен иметь Docker Compose, Python 3, Bash и доступ к registry. Постоянная папка стенда ~/support-practice-training. Секреты не коммитятся.

Для демонстрации отката сначала успешно доставьте две версии с совместимыми миграциями, затем на стенде выполните bash scripts/rollback.sh. Скрипт возвращает предыдущий образ и проверяет API. При ошибке нового релиза deploy.sh автоматически пытается восстановить предыдущий. Изменения схемы должны быть добавочными и совместимыми с предыдущим приложением.

Результаты фактических проверок и незавершённые внешние этапы перечислены в docs/verification.md.
