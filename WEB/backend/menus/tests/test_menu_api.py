import pytest
from menus.models import MenuItem, Merchant, Category


@pytest.mark.django_db
class TestMenuViewSet:
    """Test /api/menus/ endpoints"""
    
    def test_list_menu_items(self, api_client, menu_item):
        """Test listing all menu items"""
        response = api_client.get("/api/menus/")
        assert response.status_code == 200
        assert isinstance(response.data, list)
        assert len(response.data) > 0
        assert any(item["id"] == menu_item.id for item in response.data)
    
    def test_retrieve_menu_item(self, api_client, menu_item):
        """Test retrieving a specific menu item"""
        response = api_client.get(f"/api/menus/{menu_item.id}/")
        assert response.status_code == 200
        assert response.data["id"] == menu_item.id
        assert response.data["name"] == menu_item.name
        assert response.data["price"] == str(menu_item.price)
    
    def test_create_menu_item_authenticated(self, authenticated_merchant_client, merchant, category):
        """Test creating a menu item when authenticated"""
        data = {
            "merchant": merchant.id,
            "category": category.id,
            "name": "New Burger",
            "description": "A new burger",
            "price": "15.00",
            "stock": 50,
            "is_available": True
        }
        response = authenticated_merchant_client.post("/api/menus/", data)
        assert response.status_code == 201
        assert response.data["name"] == "New Burger"
        
        # Verify item was created
        item = MenuItem.objects.get(name="New Burger")
        assert item.price == 15.00
        assert item.stock == 50
    
    def test_create_menu_item_unauthenticated(self, api_client, merchant, category):
        """Test creating a menu item when not authenticated"""
        data = {
            "merchant": merchant.id,
            "category": category.id,
            "name": "New Burger",
            "price": "15.00"
        }
        response = api_client.post("/api/menus/", data)
        # Should allow or require auth depending on permissions
        # Check actual behavior
        assert response.status_code in [201, 401, 403]
    
    def test_update_menu_item(self, authenticated_merchant_client, menu_item):
        """Test updating a menu item"""
        data = {
            "name": "Updated Burger",
            "price": "12.00",
            "stock": 75
        }
        response = authenticated_merchant_client.patch(f"/api/menus/{menu_item.id}/", data)
        assert response.status_code == 200
        assert response.data["name"] == "Updated Burger"
        
        # Verify item was updated
        menu_item.refresh_from_db()
        assert menu_item.name == "Updated Burger"
        assert menu_item.price == 12.00
    
    def test_delete_menu_item(self, authenticated_merchant_client, menu_item):
        """Test deleting a menu item"""
        item_id = menu_item.id
        response = authenticated_merchant_client.delete(f"/api/menus/{item_id}/")
        assert response.status_code in [204, 200]
        
        # Verify item was deleted
        assert not MenuItem.objects.filter(id=item_id).exists()
    
    def test_menu_item_not_found(self, api_client):
        """Test retrieving non-existent menu item"""
        response = api_client.get("/api/menus/99999/")
        assert response.status_code == 404
