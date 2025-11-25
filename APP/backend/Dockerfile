# ------------ Build stage ------------
FROM node:20-alpine AS build
WORKDIR /usr/src/app

# Toolchain cho native addons (bcrypt/argon2/…)
RUN apk add --no-cache python3 make g++ ca-certificates libc6-compat

# Copy manifest trước để tối ưu cache
COPY package.json package-lock.json* ./

# Cài deps:
# - Ưu tiên npm ci (nhanh/định xác)
# - Nếu lockfile lệch -> tự fallback sang npm install để tránh build fail
RUN if [ -f package-lock.json ]; then \
      npm ci --omit=dev || (echo "npm ci failed -> fallback npm install" && npm install --omit=dev) ; \
    else \
      npm install --omit=dev ; \
    fi

# Copy source
COPY . .

# (nếu có build step TS/Webpack thì bật ở đây)
# RUN npm run build

# ------------ Runtime stage ------------
FROM node:20-alpine AS runtime
WORKDIR /usr/src/app

# Tini để xử lý tín hiệu PID1 gọn (graceful shutdown) + curl cho healthcheck
RUN apk add --no-cache tini curl

# Copy node_modules + source từ build stage
COPY --from=build /usr/src/app/node_modules ./node_modules
COPY --from=build /usr/src/app ./

ENV NODE_ENV=production \
    PORT=8000

EXPOSE 8000

# HEALTHCHECK: dùng curl (đã cài) thay vì wget
HEALTHCHECK --interval=30s --timeout=5s --retries=5 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/" || exit 1

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["node", "index.js"]
