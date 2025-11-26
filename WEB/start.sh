#!/bin/bash
set -e

cd /app/backend

# Chạy migrations
echo "Running migrations..."
python manage.py migrate --noinput || echo "Migrations failed, continuing..."

# Collect static files
echo "Collecting static files..."
python manage.py collectstatic --noinput || echo "Static files collection failed, continuing..."

# Start Gunicorn
echo "Starting Gunicorn..."
PORT=${PORT:-8000}
exec gunicorn core.wsgi:application \
    --bind 0.0.0.0:$PORT \
    --workers 2 \
    --threads 2 \
    --timeout 120 \
    --access-logfile - \
    --error-logfile - \
    --log-level info

