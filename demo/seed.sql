INSERT INTO tickets(id,title,description,category_id,priority,status,created_at,due_at)
VALUES ('10000000-0000-0000-0000-000000000001','Демонстрационная заявка','Не печатает принтер',1,'HIGH','NEW','2026-01-01T00:00:00Z','2026-01-02T00:00:00Z')
ON CONFLICT DO NOTHING;
INSERT INTO status_history(ticket_id,old_status,new_status,changed_at)
SELECT id,NULL,'NEW',created_at FROM tickets t
WHERE id='10000000-0000-0000-0000-000000000001'
AND NOT EXISTS (SELECT 1 FROM status_history h WHERE h.ticket_id=t.id);
