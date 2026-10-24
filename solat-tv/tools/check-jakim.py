"""Exercise the official current-month response before distributing an APK."""
import calendar
import datetime as dt
import json
from pathlib import Path
import urllib.request
from zoneinfo import ZoneInfo

today = dt.datetime.now(ZoneInfo("Asia/Kuala_Lumpur")).date()
url = ("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat"
       f"&period=month&zone=TRG01&year={today.year}&month={today.month}")
request = urllib.request.Request(url, headers={
    "Accept": "application/json",
    "User-Agent": "WaktuSolatTV/1.0 (personal Android TV screensaver)",
})
with urllib.request.urlopen(request, timeout=30) as response:
    assert response.status == 200
    payload = response.read(1_048_577)
assert len(payload) <= 1_048_576
data = json.loads(payload)
assert data.get("zone") == "TRG01", "Unexpected JAKIM zone"
assert str(data.get("status", "")).upper().startswith("OK"), "JAKIM error response"
keys = ("fajr", "syuruk", "dhuhr", "asr", "maghrib", "isha")
seen = set()
for row in data["prayerTime"]:
    try:
        date = dt.datetime.strptime(row["date"], "%d-%b-%Y").date()
    except ValueError:
        date = dt.date.fromisoformat(row["date"])
    assert (date.year, date.month) == (today.year, today.month), "Wrong month"
    assert date not in seen, "Duplicate date"
    seen.add(date)
    times = [dt.time.fromisoformat(row[key]) for key in keys]
    assert all(a < b for a, b in zip(times, times[1:])), "Unordered times"
    if date == today:
        print("JAKIM TRG01", date, {key: row[key] for key in keys})
assert len(seen) == calendar.monthrange(today.year, today.month)[1], "Incomplete month"
Path("build").mkdir(exist_ok=True)
Path("build/jakim-live.json").write_bytes(payload)
print("PASS: official JAKIM current-month dates, zone and prayer-time schema")
