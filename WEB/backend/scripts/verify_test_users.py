#!/usr/bin/env python3
import os, sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.dev')
import django
django.setup()

import json
from urllib import request, parse, error

USERS = ['customer1', 'merchant1', 'shipper1', 'admin1']
PW = 'Password123'

def post_json(url, payload):
    data = json.dumps(payload).encode('utf-8')
    req = request.Request(url, data=data, headers={'Content-Type': 'application/json'})
    try:
        with request.urlopen(req) as resp:
            return resp.read().decode('utf-8'), resp.getcode()
    except error.HTTPError as e:
        return e.read().decode('utf-8'), e.code

def get_url(url, headers=None):
    req = request.Request(url, headers=headers or {})
    try:
        with request.urlopen(req) as resp:
            return resp.read().decode('utf-8'), resp.getcode()
    except error.HTTPError as e:
        return e.read().decode('utf-8'), e.code

def login_and_me(username):
    url = 'http://127.0.0.1:8000/api/accounts/login/'
    body, status = post_json(url, {'username': username, 'password': PW})
    if status != 200:
        print(f'{username}: login failed {status} {body}')
        return
    obj = json.loads(body)
    access = obj.get('access')
    me_body, me_status = get_url('http://127.0.0.1:8000/api/accounts/me/', headers={'Authorization': f'Bearer {access}'})
    print(f'{username} -> {me_status} {me_body}')

def main():
    for u in USERS:
        login_and_me(u)

if __name__ == '__main__':
    main()
