#!/bin/bash

# FastFood Deployment Script for VPS
# Usage: ./deploy.sh

set -e  # Exit on error

echo "🚀 Starting FastFood deployment..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${RED}❌ Error: .env file not found!${NC}"
    echo "Please create .env file from .env.production.example"
    exit 1
fi

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Error: Docker is not installed!${NC}"
    exit 1
fi

# Check if docker compose is available
if ! docker compose version &> /dev/null; then
    echo -e "${RED}❌ Error: Docker Compose is not installed!${NC}"
    exit 1
fi

# Pull latest code (if using git)
if [ -d .git ]; then
    echo -e "${YELLOW}📥 Pulling latest code...${NC}"
    git pull || echo "Warning: Could not pull from git"
fi

# Build and start services
echo -e "${YELLOW}🔨 Building Docker images...${NC}"
docker compose -f docker-compose.prod.yml build

echo -e "${YELLOW}🛑 Stopping existing services...${NC}"
docker compose -f docker-compose.prod.yml down

echo -e "${YELLOW}🚀 Starting services...${NC}"
docker compose -f docker-compose.prod.yml up -d

# Wait for database to be ready
echo -e "${YELLOW}⏳ Waiting for database...${NC}"
sleep 10

# Run migrations
echo -e "${YELLOW}📊 Running database migrations...${NC}"
docker compose -f docker-compose.prod.yml exec -T backend python manage.py migrate --noinput

# Collect static files
echo -e "${YELLOW}📦 Collecting static files...${NC}"
docker compose -f docker-compose.prod.yml exec -T backend python manage.py collectstatic --noinput

# Show status
echo -e "${GREEN}✅ Deployment completed!${NC}"
echo ""
echo "📊 Service status:"
docker compose -f docker-compose.prod.yml ps

echo ""
echo -e "${GREEN}🌐 Your application should be available at:${NC}"
echo "   - Frontend: http://your-domain.com"
echo "   - Backend API: http://your-domain.com/api"
echo "   - Admin: http://your-domain.com/admin"
echo ""
echo "📝 To view logs:"
echo "   docker compose -f docker-compose.prod.yml logs -f"
echo ""
echo "🔐 To create superuser:"
echo "   docker compose -f docker-compose.prod.yml exec backend python manage.py createsuperuser"

