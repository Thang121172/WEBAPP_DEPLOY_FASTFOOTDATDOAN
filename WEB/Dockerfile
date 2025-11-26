FROM python:3.11-slim

# Thiết lập thư mục làm việc trong container
WORKDIR /app

# Biến môi trường
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=on \
    PIP_DISABLE_PIP_VERSION_CHECK=on \
    PYTHONPATH=/app/backend

# Cài đặt hệ thống dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements và cài đặt Python dependencies
COPY backend/requirements.txt /app/requirements.txt
RUN python -m pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy mã nguồn backend
COPY backend /app/backend/

# Copy script khởi động
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

# Tạo thư mục cho static files và media
RUN mkdir -p /app/backend/staticfiles /app/backend/media

# Expose port (Render sẽ tự động set PORT env var)
EXPOSE 8000

# Sử dụng script khởi động
CMD ["/app/start.sh"]