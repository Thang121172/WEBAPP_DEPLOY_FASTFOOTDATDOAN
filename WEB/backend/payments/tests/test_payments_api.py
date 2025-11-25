import pytest


@pytest.mark.django_db
class TestCreatePaymentView:
    """Test /api/payments/create/ endpoint"""
    
    def test_create_payment(self, api_client):
        """Test creating a payment"""
        data = {
            "order_id": 1,
            "amount": "100.00"
        }
        response = api_client.post("/api/payments/create/", data, format="json")
        assert response.status_code == 200
        assert "payment_url" in response.data
    
    def test_create_payment_no_data(self, api_client):
        """Test creating payment without data"""
        response = api_client.post("/api/payments/create/")
        assert response.status_code == 200


@pytest.mark.django_db
class TestPaymentCallbackView:
    """Test /api/payments/callback/ endpoint"""
    
    def test_payment_callback(self, api_client):
        """Test payment callback"""
        data = {
            "payment_id": "test123",
            "status": "success"
        }
        response = api_client.post("/api/payments/callback/", data, format="json")
        assert response.status_code == 200
        assert response.data["status"] == "ok"
    
    def test_payment_callback_no_data(self, api_client):
        """Test payment callback without data"""
        response = api_client.post("/api/payments/callback/")
        assert response.status_code == 200

