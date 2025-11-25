FROM node:20-alpine
WORKDIR /app

# install deps
COPY package.json package-lock.json* ./
RUN npm ci --silent || npm install --silent

# copy source
COPY . /app

EXPOSE 5173

CMD ["sh", "-c", "npm run dev -- --host 0.0.0.0"]
