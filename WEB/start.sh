#!/bin/bash
set -e

cd /app/backend

# Debug: Print environment
echo "=== Environment ==="
echo "DJANGO_SETTINGS_MODULE: ${DJANGO_SETTINGS_MODULE:-not set}"
echo "PORT: ${PORT:-8000}"
echo "PYTHONPATH: ${PYTHONPATH:-not set}"
echo "==================="

# Chạy migrations
echo "Running migrations..."
python manage.py migrate --noinput || {
    echo "WARNING: Migrations failed, but continuing..."
}

# Collect static files
echo "Collecting static files..."
python manage.py collectstatic --noinput || {
    echo "WARNING: Static files collection failed, but continuing..."
}

# Test Django setup
echo "Testing Django setup..."
python manage.py check --deploy || {
    echo "WARNING: Django check failed, but continuing..."
}

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
    --log-level info \
    --capture-output

