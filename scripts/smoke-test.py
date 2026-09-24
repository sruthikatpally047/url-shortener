#!/usr/bin/env python3
"""Exercise a running server without following redirects or contacting destination URLs."""
import json
import os
import statistics
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor

BASE = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def request(method, path, payload=None, expected=200):
    data = None if payload is None else json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                 headers={"Content-Type": "application/json"})
    opener = urllib.request.build_opener(NoRedirect)
    try:
        response = opener.open(req, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        body = response.read()
        assert response.status == expected, (method, path, response.status, body)
        parsed = json.loads(body) if body and "json" in response.headers.get("Content-Type", "") else None
        return parsed, response.headers

request("GET", "/actuator/health")
request("GET", "/swagger-ui/index.html")
spec, _ = request("GET", "/v3/api-docs")
assert spec["info"]["title"] == "URL Shortener API"
alias = "smoke-" + uuid.uuid4().hex[:12]
link, headers = request("POST", "/api/v1/links", {
    "originalUrl": "https://example.com/path?q=one#section", "customAlias": alias
}, 201)
assert headers["Location"] == "/api/v1/links/" + alias
assert link["shortUrl"].endswith("/r/" + alias)
request("HEAD", "/r/" + alias, expected=302)
analytics, _ = request("GET", "/api/v1/links/" + alias + "/analytics")
assert analytics["totalClicks"] == 0

def redirect(_):
    start = time.perf_counter()
    _, headers = request("GET", "/r/" + alias, expected=302)
    assert headers["Location"] == "https://example.com/path?q=one#section"
    assert headers["Cache-Control"] == "no-store"
    return (time.perf_counter() - start) * 1000

with ThreadPoolExecutor(max_workers=8) as pool:
    latencies = list(pool.map(redirect, range(80)))
analytics, _ = request("GET", "/api/v1/links/" + alias + "/analytics")
assert analytics["totalClicks"] == 80
request("POST", "/api/v1/links", {"originalUrl": "https://example.org", "customAlias": alias}, 409)
request("POST", "/api/v1/links", {"originalUrl": "javascript:alert(1)"}, 400)
request("POST", "/api/v1/links", {"originalUrl": "https://example.org", "customAlias": "bad alias"}, 400)
request("GET", "/r/missing-" + uuid.uuid4().hex, expected=404)
request("DELETE", "/api/v1/links/" + alias, expected=204)
request("DELETE", "/api/v1/links/" + alias, expected=204)
request("GET", "/r/" + alias, expected=410)
analytics, _ = request("GET", "/api/v1/links/" + alias + "/analytics")
assert analytics["totalClicks"] == 80
print(json.dumps({"result": "PASS", "baseUrl": BASE, "code": alias, "concurrentRequests": 80,
                  "workers": 8, "recordedClicks": analytics["totalClicks"],
                  "medianRedirectMs": round(statistics.median(latencies), 2),
                  "p95RedirectMs": round(sorted(latencies)[75], 2)}, indent=2))
