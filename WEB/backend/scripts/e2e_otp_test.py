"""
Simple end-to-end OTP flow test (dev only).
Sends requests to the running backend at BASE_URL (default: http://127.0.0.1:8000/api).

Usage (from repo root):
    python backend/scripts/e2e_otp_test.py

This is intentionally dependency-free (uses urllib) so it runs with system Python.
"""
import json
import time
import argparse
import urllib.request
import urllib.error


def post(url, data):
    data_b = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=data_b, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        try:
            body = e.read().decode("utf-8")
            return {"_error_status": e.code, "_error_body": body}
        except Exception:
            return {"_error_status": e.code}


def get(url):
    try:
        with urllib.request.urlopen(url) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        try:
            body = e.read().decode("utf-8")
            return {"_error_status": e.code, "_error_body": body}
        except Exception:
            return {"_error_status": e.code}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://127.0.0.1:8000/api", help="Base API URL (default http://127.0.0.1:8000/api)")
    args = parser.parse_args()
    base = args.base.rstrip("/")

    now = int(time.time())
    username = f"testflow{now}"
    email = f"{username}@example.com"
    password = "Password123"

    print("Test user:", username, email)

    print('\n1) Registering user')
    reg = post(f"{base}/accounts/register/", {"username": username, "email": email, "password": password})
    print(json.dumps(reg, indent=2, ensure_ascii=False))

    print('\n2) Sending OTP')
    send = post(f"{base}/accounts/send_otp/", {"identifier": email})
    print(json.dumps(send, indent=2, ensure_ascii=False))

    print('\n3) Fetching latest dev OTP')
    latest = get(f"{base}/accounts/dev_latest_otp/?identifier={urllib.request.quote(email)}")
    print(json.dumps(latest, indent=2, ensure_ascii=False))

    code = latest.get("code")
    if not code:
        print('\nERROR: no code retrieved; aborting')
        raise SystemExit(2)

    print('\n4) Verifying OTP (using code from dev_latest_otp)')
    verify = post(f"{base}/accounts/verify_otp/", {"identifier": email, "code": code})
    print(json.dumps(verify, indent=2, ensure_ascii=False))

    print('\n5) Confirming OTP state after verify')
    final = get(f"{base}/accounts/dev_latest_otp/?identifier={urllib.request.quote(email)}")
    print(json.dumps(final, indent=2, ensure_ascii=False))

    print('\nE2E OTP test finished.')
