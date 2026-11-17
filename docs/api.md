# REST-контракт
Все тела запросов и ответов — JSON. Даты — ISO-8601 UTC. UUID берётся из ответа создания.
| Метод | Путь | Результат |
|---|---|---|
| POST | /api/tickets | 201 и Ticket |
| GET | /api/tickets/{id} | 200 и Ticket |
| PUT | /api/tickets/{id} | 200 и обновлённый Ticket |
| PATCH | /api/tickets/{id}/status | 200 и Ticket |
| GET | /api/tickets | 200, content,total,page,size |
| GET | /api/tickets/{id}/history | 200, массив old_status,new_status,changed_at |
| GET | /api/categories | 200, массив id,name |
| GET | /api/reports/summary?from=...&to=... | 200 и Summary |

Создание:
```json
{"title":"Не работает принтер","description":"Ошибка подключения","categoryId":1,"priority":"HIGH","dueAt":"2027-01-01T12:00:00Z"}
```
Ответ:
```json
{"id":"00000000-0000-0000-0000-000000000001","title":"Не работает принтер","description":"Ошибка подключения","categoryId":1,"priority":"HIGH","status":"NEW","createdAt":"2026-11-17T12:00:00Z","dueAt":"2027-01-01T12:00:00Z","closedAt":null}
```
Редактирование:
```json
{"description":"Уточнено","priority":"MEDIUM","dueAt":null}
```
Переход:
```json
{"status":"IN_PROGRESS"}
```
Пустой отчёт:
```json
{"from":"2026-01-01T00:00:00Z","to":"2026-02-01T00:00:00Z","createdCount":0,"byStatus":{"NEW":0,"IN_PROGRESS":0,"RESOLVED":0,"CANCELLED":0},"byPriority":{"LOW":0,"MEDIUM":0,"HIGH":0},"overdueOpenCount":0,"averageResolutionSeconds":null}
```
Ошибки — 400 при неверных данных/периоде, 404 для отсутствующей заявки, 409 при запрещённой операции. Полный генерируемый OpenAPI доступен в /v3/api-docs, интерактивная проверка — Swagger UI. Коллекция demo/requests.http.
