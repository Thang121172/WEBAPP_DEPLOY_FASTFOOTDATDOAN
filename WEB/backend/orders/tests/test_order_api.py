import pytest
from orders.models import Order, OrderItem
from menus.models import MenuItem


@pytest.mark.django_db
class TestOrderViewSet:
    """Test /api/orders/ endpoints"""
    
    def test_list_orders_authenticated(self, authenticated_customer_client, order):
        """Test listing orders for authenticated customer"""
        response = authenticated_customer_client.get("/api/orders/")
        assert response.status_code == 200
        assert isinstance(response.data, list)
        assert len(response.data) > 0
        assert any(o["id"] == order.id for o in response.data)
    
    def test_list_orders_unauthenticated(self, api_client):
        """Test listing orders when not authenticated"""
        response = api_client.get("/api/orders/")
        assert response.status_code == 401
    
    def test_retrieve_order_own(self, authenticated_customer_client, order):
        """Test retrieving own order"""
        response = authenticated_customer_client.get(f"/api/orders/{order.id}/")
        assert response.status_code == 200
        assert response.data["id"] == order.id
        assert response.data["status"] == order.status
        assert "items" in response.data
    
    def test_retrieve_order_not_own(self, authenticated_customer_client, customer_user):
        """Test retrieving order that doesn't belong to user"""
        # Create another user and order
        from django.contrib.auth import get_user_model
        User = get_user_model()
        other_user = User.objects.create_user(
            username="otheruser",
            email="other@test.com",
            password="testpass123"
        )
        from accounts.models import Profile
        Profile.objects.create(user=other_user, role="customer")
        
        from menus.models import Merchant
        merchant = Merchant.objects.create(
            owner=other_user,
            name="Other Restaurant",
            address="123 St",
            phone="0123456780"
        )
        
        other_order = Order.objects.create(
            customer=other_user,
            merchant=merchant,
            status=Order.Status.PENDING,
            payment_status=Order.PaymentStatus.UNPAID,
            total_amount=10.00
        )
        
        response = authenticated_customer_client.get(f"/api/orders/{other_order.id}/")
        assert response.status_code == 404
    
    def test_create_order_success(self, authenticated_customer_client, merchant, menu_item):
        """Test creating a new order"""
        data = {
            "merchant_id": merchant.id,
            "delivery_address": "123 Delivery St",
            "note": "Please hurry",
            "items": [
                {
                    "menu_item_id": menu_item.id,
                    "quantity": 2
                }
            ]
        }
        response = authenticated_customer_client.post("/api/orders/", data, format="json")
        assert response.status_code == 201
        assert response.data["status"] == Order.Status.PENDING
        assert response.data["delivery_address"] == "123 Delivery St"
        assert len(response.data["items"]) == 1
        
        # Verify order was created
        order = Order.objects.get(id=response.data["id"])
        assert order.total_amount > 0
        assert order.items.count() == 1
    
    def test_create_order_invalid_merchant(self, authenticated_customer_client):
        """Test creating order with invalid merchant"""
        data = {
            "merchant_id": 99999,
            "delivery_address": "123 St",
            "items": []
        }
        response = authenticated_customer_client.post("/api/orders/", data, format="json")
        assert response.status_code == 400
    
    def test_create_order_invalid_menu_item(self, authenticated_customer_client, merchant):
        """Test creating order with invalid menu item"""
        data = {
            "merchant_id": merchant.id,
            "delivery_address": "123 St",
            "items": [
                {
                    "menu_item_id": 99999,
                    "quantity": 1
                }
            ]
        }
        response = authenticated_customer_client.post("/api/orders/", data, format="json")
        assert response.status_code == 400
    
    def test_set_order_status(self, authenticated_customer_client, order):
        """Test setting order status"""
        data = {
            "status": Order.Status.CONFIRMED
        }
        response = authenticated_customer_client.post(
            f"/api/orders/{order.id}/set_status/",
            data,
            format="json"
        )
        assert response.status_code == 200
        assert response.data["status"] == Order.Status.CONFIRMED
        
        # Verify status was updated
        order.refresh_from_db()
        assert order.status == Order.Status.CONFIRMED
    
    def test_create_order_merchant_role(self, authenticated_merchant_client, merchant, menu_item):
        """Test that merchant cannot create orders"""
        data = {
            "merchant_id": merchant.id,
            "delivery_address": "123 St",
            "items": [
                {
                    "menu_item_id": menu_item.id,
                    "quantity": 1
                }
            ]
        }
        response = authenticated_merchant_client.post("/api/orders/", data, format="json")
        # Should be forbidden for merchant role
        assert response.status_code in [403, 400]


@pytest.mark.django_db
class TestMerchantViewSet:
    """Test /api/merchant/ endpoints"""
    
    def test_list_menu_items(self, authenticated_merchant_client, menu_item):
        """Test listing menu items for merchant"""
        response = authenticated_merchant_client.get("/api/merchant/")
        assert response.status_code == 200
        assert isinstance(response.data, list)
        assert any(item["id"] == menu_item.id for item in response.data)
        # Should include stock field
        item_data = next(item for item in response.data if item["id"] == menu_item.id)
        assert "stock" in item_data
    
    def test_update_stock_success(self, authenticated_merchant_client, menu_item):
        """Test updating menu item stock"""
        data = {
            "stock": 150
        }
        response = authenticated_merchant_client.post(
            f"/api/merchant/{menu_item.id}/update_stock/",
            data,
            format="json"
        )
        assert response.status_code == 200
        assert response.data["stock"] == 150
        
        # Verify stock was updated
        menu_item.refresh_from_db()
        assert menu_item.stock == 150
    
    def test_update_stock_invalid_item(self, authenticated_merchant_client):
        """Test updating stock for non-existent item"""
        data = {
            "stock": 100
        }
        response = authenticated_merchant_client.post(
            "/api/merchant/99999/update_stock/",
            data,
            format="json"
        )
        assert response.status_code == 404
    
    def test_update_stock_invalid_value(self, authenticated_merchant_client, menu_item):
        """Test updating stock with invalid value"""
        data = {
            "stock": "invalid"
        }
        response = authenticated_merchant_client.post(
            f"/api/merchant/{menu_item.id}/update_stock/",
            data,
            format="json"
        )
        assert response.status_code == 400


@pytest.mark.django_db
class TestShipperViewSet:
    """Test /api/shipper/ endpoints"""
    
    def test_list_orders(self, authenticated_shipper_client, order):
        """Test listing orders for shipper"""
        response = authenticated_shipper_client.get("/api/shipper/")
        assert response.status_code == 200
        assert isinstance(response.data, list)
        # Should include orders that are not DELIVERED
        assert any(o["id"] == order.id for o in response.data)
    
    def test_pickup_order_success(self, authenticated_shipper_client, order):
        """Test shipper picking up an order"""
        response = authenticated_shipper_client.post(f"/api/shipper/{order.id}/pickup/")
        assert response.status_code == 200
        assert response.data["status"] == Order.Status.DELIVERING
        assert response.data["shipper_id"] is not None
        
        # Verify order was updated
        order.refresh_from_db()
        assert order.status == Order.Status.DELIVERING
        assert order.shipper is not None
    
    def test_pickup_order_not_found(self, authenticated_shipper_client):
        """Test picking up non-existent order"""
        response = authenticated_shipper_client.post("/api/shipper/99999/pickup/")
        assert response.status_code == 404


@pytest.mark.django_db
class TestMerchantDashboard:
    """Test /api/merchant/dashboard/ endpoint"""
    
    def test_dashboard_success(self, authenticated_merchant_client, merchant, order):
        """Test merchant dashboard"""
        response = authenticated_merchant_client.get("/api/merchant/dashboard/")
        assert response.status_code == 200
        assert "merchant" in response.data
        assert "orders_today" in response.data
        assert "revenue_today" in response.data
        assert "sold_out" in response.data
        assert "recent_orders" in response.data
    
    def test_dashboard_not_merchant(self, authenticated_customer_client):
        """Test dashboard access for non-merchant"""
        response = authenticated_customer_client.get("/api/merchant/dashboard/")
        assert response.status_code == 403
    
    def test_dashboard_unauthenticated(self, api_client):
        """Test dashboard access when not authenticated"""
        response = api_client.get("/api/merchant/dashboard/")
        assert response.status_code == 401
