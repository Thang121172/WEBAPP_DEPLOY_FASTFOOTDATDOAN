# FASTFOOD Monorepo

A full-stack fast food ordering application with Django backend and React frontend.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose

## Project Structure

- `backend/`: Django REST API
- `frontend/`: React application with Vite
- `docker-compose.yml`: Orchestrates services (DB, Backend, Frontend, Mailhog)

## Project tree (code files)

Below is a concise tree of the repository showing the important code folders and files (generated from the workspace). I excluded generated folders like `node_modules/` and `__pycache__/` for readability.
## Project tree (exhaustive, filtered)

Below is a comprehensive listing of repository files (excludes any path containing `middleware`, `venv`, `.venv`, `__pycache__`, or `node_modules`). Paths are shown relative to the repository root.

```
.dockerignore
.pre-commit-config.yaml
docker-compose.yml
Makefile
project_files.txt
README.md
tree_dirs.txt
tree_files.txt
.github/workflows/ci.yml
.github/workflows/cd.yml

backend/.env
backend/.env.example
backend/apps.py
backend/Dockerfile
backend/manage.py
backend/pytest.ini
backend/requirements.txt
backend/signals.py

backend/core/__init__.py
backend/core/asgi.py
backend/core/app.py
backend/core/health.py
backend/core/logging.py
backend/core/routing.py
backend/core/sentry.py
backend/core/settings.py
backend/core/swagger.py
backend/core/urls_root.py
backend/core/urls.py
backend/core/wsgi.py
backend/core/settings/__init__.py
backend/core/settings/base.py
backend/core/settings/dev.py
backend/core/settings/prod.py

backend/accounts/__init__.py
backend/accounts/app.py
backend/accounts/auth.py
backend/accounts/models.py
backend/accounts/permissions.py
backend/accounts/serializers.py
backend/accounts/urls.py
backend/accounts/views.py

backend/menus/__init__.py
backend/menus/models.py
backend/menus/serializers.py
backend/menus/urls.py
backend/menus/views.py
backend/menus/tests/test_menu_api.py

backend/orders/__init__.py
backend/orders/consumers.py
backend/orders/models.py
backend/orders/serializers.py
backend/orders/tasks.py
backend/orders/urls.py
backend/orders/views.py
backend/orders/tests/test_order_api.py

backend/payments/__init__.py
backend/payments/gateways.py
backend/payments/urls.py
backend/payments/views.py

backend/management/commands/seed_demo.py

backend/scripts/collect_error.py
backend/scripts/create_missing_auth_tables.py
backend/scripts/create_test_user.py
backend/scripts/create_test_users.py
backend/scripts/e2e_otp_test.py
backend/scripts/inspect_schema.py
backend/scripts/inspect_tables.py
backend/scripts/list_models.py
backend/scripts/print_migrations.py
backend/scripts/test_login_client.py
backend/scripts/test_login_flow.py
backend/scripts/verify_test_users.py

backend/tests/conftest.py
backend/tests/factories.py

docs/API_GUIDE.md
docs/ARCHITECTURE.md
docs/OPS_RUNBOOK.md

frontend/.env
frontend/Dockerfile
frontend/index.html
frontend/package.json
frontend/package-lock.json
frontend/postcss.config.cjs
frontend/service-worker.ts
frontend/tailwind.config.cjs
frontend/tsconfig.json
frontend/vite.config.ts
frontend/docs/MAPS.md

frontend/src/main.tsx
frontend/src/App.tsx
frontend/src/index.css
frontend/src/vite-env.d.ts

frontend/src/context/AuthContext.tsx

frontend/src/hooks/useAuth.ts
frontend/src/hooks/useLocalStorage.ts
frontend/src/hooks/useOrderWS.ts

frontend/src/components/Header.tsx
frontend/src/components/MenuCard.tsx
frontend/src/components/ProtectedRoute.tsx
frontend/src/components/forms/LoginForm.tsx
frontend/src/components/FormCard.tsx
frontend/src/components/SomeOtherComponent.tsx

frontend/src/pages/Home.tsx
frontend/src/pages/Login.tsx
frontend/src/pages/Register.tsx
frontend/src/pages/Cart.tsx
frontend/src/pages/ForgotPassword.tsx
frontend/src/pages/index.tsx

frontend/src/pages/Customer/
   - CustomerCheckout.tsx
   - CustomerOrderHistory.tsx
   - CustomerOrderTracking.tsx

frontend/src/pages/Merchant/
   - MerchantConfirmOrder.tsx
   - MerchantDashboard.tsx
   - MerchantEditProduct.tsx
   - MerchantMenu.tsx
   - MerchantOrders.tsx
   - MerchantReports.tsx
   - MerchantSettings.tsx
   - RegisterStore.tsx

frontend/src/pages/Shipper/
   - (directory not present in workspace) — if you have shipper pages, provide the files or let me know and I will add them here

frontend/src/pages/Order.tsx
frontend/src/pages/Payment.tsx
frontend/src/pages/Payment/Success.tsx
frontend/src/pages/Shipping.tsx
frontend/src/pages/Verify.tsx

frontend/src/pages/Admin/
   - AdminDashboard.tsx
   - AdminMerchantManagement.tsx
   - AdminShipperManagement.tsx

frontend/src/services/http.ts
frontend/src/store/cartStore.ts
frontend/src/types/api.ts
frontend/src/utils/geo.ts
frontend/src/utils/shipping.ts

ops/nginx/nginx.conf
ops/prometheus/alerts.yml
ops/prometheus/prometheus.yml
ops/scripts/backup_db.sh

postman/fastfood_collection.json
thunder-client/collection.json

```
   │  ├─ views.py
   │  └─ serializers.py
   ├─ menus/
   │  ├─ models.py
   │  ├─ views.py
   │  └─ serializers.py
   ├─ orders/
   │  ├─ models.py
   │  ├─ views.py
   │  ├─ consumers.py
   │  └─ tasks.py
   └─ payments/
      ├─ gateways.py
      └─ views.py
```

## Services

- **Database**: PostgreSQL on port 5433
- **Backend**: Django API on port 8000
- **Frontend**: React app on port 5173
- **Mailhog**: Email testing on ports 8025 (web) and 1025 (SMTP)

## Environment Variables

Create a `.env` file in the `backend/` directory with the following variables (defaults provided in docker-compose.yml):

```
POSTGRES_DB=fastfood
POSTGRES_USER=app
POSTGRES_PASSWORD=123456
EMAIL_HOST=mailhog
EMAIL_PORT=1025
```

## Running the Project

1. Ensure Docker Desktop is running.
2. From the root directory, run:
   ```bash
   docker-compose up
   ```
3. Access the services:
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8000
   - Mailhog: http://localhost:8025
   - Database: localhost:5433

## Development

- Backend migrations run automatically on startup.
- Frontend uses Vite for hot reloading.
- Use `docker-compose down` to stop services.

## Testing

Run backend tests:
```bash
docker-compose exec backend python manage.py test
```

## API Documentation

Swagger UI available at: http://localhost:8000/swagger/
