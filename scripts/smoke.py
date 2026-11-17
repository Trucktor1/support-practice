import json
import urllib.request
import urllib.error
from datetime import datetime, timezone, timedelta
base = "http://localhost:8080"
def request(path, data=None, method="GET"):
    body = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(base + path, body, {"Content-Type": "application/json"}, method=method)
    with urllib.request.urlopen(req, timeout=15) as response:
        return json.load(response)
assert request("/actuator/health/readiness")["status"] == "UP"
ticket = request("/api/tickets", {"title":"Smoke check","description":"Pipeline verification","categoryId":1,"priority":"LOW"}, "POST")
path = "/api/tickets/" + ticket["id"]
for status in ["IN_PROGRESS", "RESOLVED"]:
    assert request(path + "/status", {"status":status}, "PATCH")["status"] == status
assert len(request(path + "/history")) == 3
try:
    request(path + "/status", {"status":"NEW"}, "PATCH")
    raise AssertionError("Closed ticket was reopened")
except urllib.error.HTTPError as error:
    assert error.code == 409
now = datetime.now(timezone.utc)
start = (now - timedelta(days=1)).isoformat().replace("+00:00", "Z")
end = (now + timedelta(days=1)).isoformat().replace("+00:00", "Z")
assert request("/api/reports/summary?from=" + start + "&to=" + end)["createdCount"] >= 1
print("PASS lifecycle, history, conflict and Kotlin report")
