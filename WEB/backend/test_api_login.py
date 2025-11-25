#!/usr/bin/env python3
"""
Test API login endpoint truc tiep
"""
import requests
import json

API_BASE = "http://localhost:8000/api"

# Test data
email = "tesstaccc@gmail.com"
password = "Thang2004"

print("=" * 60)
print("TEST API LOGIN ENDPOINT")
print("=" * 60)
print()

# Test 1: Login voi email va username (nhu frontend)
print("Test 1: POST /api/accounts/login/ voi email va username")
payload1 = {
    "email": email,
    "username": email,  # Frontend gui ca hai
    "password": password
}
print(f"Payload: {json.dumps({**payload1, 'password': '***'}, indent=2)}")
try:
    response = requests.post(f"{API_BASE}/accounts/login/", json=payload1)
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"[OK] Login thanh cong!")
        print(f"Response: {json.dumps(data, indent=2)}")
    else:
        print(f"[LOI] Login that bai!")
        print(f"Response: {response.text}")
except Exception as e:
    print(f"[LOI] Exception: {e}")
print()

# Test 2: Login chi voi email
print("Test 2: POST /api/accounts/login/ chi voi email")
payload2 = {
    "email": email,
    "password": password
}
print(f"Payload: {json.dumps({**payload2, 'password': '***'}, indent=2)}")
try:
    response = requests.post(f"{API_BASE}/accounts/login/", json=payload2)
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"[OK] Login thanh cong!")
        print(f"Response: {json.dumps(data, indent=2)}")
    else:
        print(f"[LOI] Login that bai!")
        print(f"Response: {response.text}")
except Exception as e:
    print(f"[LOI] Exception: {e}")
print()

# Test 3: Login voi password sai
print("Test 3: POST /api/accounts/login/ voi password sai")
payload3 = {
    "email": email,
    "password": "wrong_password"
}
print(f"Payload: {json.dumps({**payload3, 'password': '***'}, indent=2)}")
try:
    response = requests.post(f"{API_BASE}/accounts/login/", json=payload3)
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        print(f"[OK] Login thanh cong (khong mong doi!)")
    else:
        print(f"[OK] Login that bai (mong doi!)")
        print(f"Response: {response.text}")
except Exception as e:
    print(f"[LOI] Exception: {e}")

