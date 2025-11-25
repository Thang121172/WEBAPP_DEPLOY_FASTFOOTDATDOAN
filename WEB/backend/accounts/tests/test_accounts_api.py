import pytest
from django.contrib.auth import get_user_model
from django.utils import timezone
from datetime import timedelta
from accounts.models import Profile, OTPRequest

User = get_user_model()


@pytest.mark.django_db
class TestRegisterView:
    """Test /api/accounts/register/ endpoint"""
    
    def test_register_success(self, api_client):
        """Test successful user registration"""
        data = {
            "username": "newuser",
            "email": "newuser@test.com",
            "password": "testpass123"
        }
        response = api_client.post("/api/accounts/register/", data)
        assert response.status_code == 201
        assert "id" in response.data
        assert response.data["username"] == "newuser"
        assert "access" in response.data
        assert "refresh" in response.data
        
        # Verify user was created
        user = User.objects.get(username="newuser")
        assert user.email == "newuser@test.com"
        assert user.profile.role == "customer"
    
    def test_register_duplicate_username(self, api_client, customer_user):
        """Test registration with duplicate username"""
        data = {
            "username": customer_user.username,
            "email": "different@test.com",
            "password": "testpass123"
        }
        response = api_client.post("/api/accounts/register/", data)
        assert response.status_code == 400
    
    def test_register_duplicate_email(self, api_client, customer_user):
        """Test registration with duplicate email"""
        data = {
            "username": "differentuser",
            "email": customer_user.email,
            "password": "testpass123"
        }
        response = api_client.post("/api/accounts/register/", data)
        assert response.status_code == 400


@pytest.mark.django_db
class TestLoginView:
    """Test /api/accounts/login/ endpoint"""
    
    def test_login_success_with_username(self, api_client, customer_user):
        """Test login with username"""
        data = {
            "username": customer_user.username,
            "password": "testpass123"
        }
        response = api_client.post("/api/accounts/login/", data)
        assert response.status_code == 200
        assert "tokens" in response.data
        assert "access" in response.data["tokens"]
        assert "refresh" in response.data["tokens"]
        assert response.data["user_id"] == customer_user.id
    
    def test_login_success_with_email(self, api_client, customer_user):
        """Test login with email"""
        data = {
            "email": customer_user.email,
            "password": "testpass123"
        }
        response = api_client.post("/api/accounts/login/", data)
        assert response.status_code == 200
        assert "tokens" in response.data
    
    def test_login_wrong_password(self, api_client, customer_user):
        """Test login with wrong password"""
        data = {
            "username": customer_user.username,
            "password": "wrongpass"
        }
        response = api_client.post("/api/accounts/login/", data)
        assert response.status_code == 400
    
    def test_login_nonexistent_user(self, api_client):
        """Test login with non-existent user"""
        data = {
            "username": "nonexistent",
            "password": "testpass123"
        }
        response = api_client.post("/api/accounts/login/", data)
        assert response.status_code == 400


@pytest.mark.django_db
class TestMeView:
    """Test /api/accounts/me/ endpoint"""
    
    def test_me_authenticated(self, authenticated_customer_client, customer_user):
        """Test getting current user info when authenticated"""
        response = authenticated_customer_client.get("/api/accounts/me/")
        assert response.status_code == 200
        assert response.data["id"] == customer_user.id
        assert response.data["username"] == customer_user.username
        assert response.data["role"] == "customer"
    
    def test_me_unauthenticated(self, api_client):
        """Test getting current user info when not authenticated"""
        response = api_client.get("/api/accounts/me/")
        assert response.status_code == 401


@pytest.mark.django_db
class TestRegisterRequestOTPView:
    """Test /api/accounts/register/request-otp/ endpoint"""
    
    def test_request_otp_success(self, api_client):
        """Test requesting OTP for registration"""
        data = {
            "email": "newuser@test.com",
            "password": "testpass123",
            "role": "customer"
        }
        response = api_client.post("/api/accounts/register/request-otp/", data)
        assert response.status_code == 200
        assert "detail" in response.data
        assert "expires_at" in response.data
        # In DEBUG mode, should return debug_otp
        if "debug_otp" in response.data:
            assert len(response.data["debug_otp"]) == 6
        
        # Verify OTP was created
        otp = OTPRequest.objects.filter(identifier="newuser@test.com").first()
        assert otp is not None
        assert otp.purpose == OTPRequest.PURPOSE_REGISTER
    
    def test_request_otp_duplicate_email(self, api_client, customer_user):
        """Test requesting OTP with existing email"""
        data = {
            "email": customer_user.email,
            "password": "testpass123",
            "role": "customer"
        }
        response = api_client.post("/api/accounts/register/request-otp/", data)
        assert response.status_code == 400


@pytest.mark.django_db
class TestRegisterConfirmOTPView:
    """Test /api/accounts/register/confirm/ endpoint"""
    
    def test_confirm_otp_success(self, api_client):
        """Test confirming OTP and completing registration"""
        # First request OTP
        email = "newuser@test.com"
        data = {
            "email": email,
            "password": "testpass123",
            "role": "customer"
        }
        request_response = api_client.post("/api/accounts/register/request-otp/", data)
        assert request_response.status_code == 200
        
        # Get OTP code (from debug_otp in dev mode or from database)
        otp_code = request_response.data.get("debug_otp")
        if not otp_code:
            otp = OTPRequest.objects.filter(identifier=email).latest("created_at")
            otp_code = otp.code
        
        # Confirm OTP
        confirm_data = {
            "email": email,
            "otp": otp_code,
            "password": "testpass123",
            "role": "customer"
        }
        response = api_client.post("/api/accounts/register/confirm/", confirm_data)
        assert response.status_code == 201
        assert "user_id" in response.data
        assert "tokens" in response.data
        assert response.data["email"] == email
        
        # Verify user was created
        user = User.objects.get(email=email)
        assert user.profile.role == "customer"
        
        # Verify OTP was marked as used
        otp = OTPRequest.objects.filter(identifier=email, code=otp_code).first()
        assert otp.used is True
    
    def test_confirm_otp_invalid_code(self, api_client):
        """Test confirming with invalid OTP code"""
        data = {
            "email": "newuser@test.com",
            "otp": "000000",
            "password": "testpass123",
            "role": "customer"
        }
        response = api_client.post("/api/accounts/register/confirm/", data)
        assert response.status_code == 400


@pytest.mark.django_db
class TestForgotPasswordRequestOTPView:
    """Test /api/accounts/forgot/request-otp/ endpoint"""
    
    def test_forgot_password_request_otp_success(self, api_client, customer_user):
        """Test requesting OTP for password reset"""
        data = {
            "email": customer_user.email
        }
        response = api_client.post("/api/accounts/forgot/request-otp/", data)
        assert response.status_code == 200
        assert "detail" in response.data
        
        # Verify OTP was created
        otp = OTPRequest.objects.filter(
            identifier=customer_user.email,
            purpose=OTPRequest.PURPOSE_RESET_PASSWORD
        ).first()
        assert otp is not None
    
    def test_forgot_password_nonexistent_email(self, api_client):
        """Test requesting OTP for non-existent email (should still return 200)"""
        data = {
            "email": "nonexistent@test.com"
        }
        response = api_client.post("/api/accounts/forgot/request-otp/", data)
        # Should return 200 to avoid leaking user information
        assert response.status_code == 200


@pytest.mark.django_db
class TestResetPasswordConfirmView:
    """Test /api/accounts/reset-password/confirm/ endpoint"""
    
    def test_reset_password_success(self, api_client, customer_user):
        """Test resetting password with valid OTP"""
        # Request OTP
        data = {
            "email": customer_user.email
        }
        request_response = api_client.post("/api/accounts/forgot/request-otp/", data)
        assert request_response.status_code == 200
        
        # Get OTP code
        otp_code = request_response.data.get("debug_otp")
        if not otp_code:
            otp = OTPRequest.objects.filter(
                identifier=customer_user.email,
                purpose=OTPRequest.PURPOSE_RESET_PASSWORD
            ).latest("created_at")
            otp_code = otp.code
        
        # Reset password
        reset_data = {
            "email": customer_user.email,
            "otp": otp_code,
            "new_password": "newpass123"
        }
        response = api_client.post("/api/accounts/reset-password/confirm/", reset_data)
        assert response.status_code == 200
        
        # Verify password was changed
        user = User.objects.get(id=customer_user.id)
        assert user.check_password("newpass123")
        
        # Verify OTP was marked as used
        otp = OTPRequest.objects.filter(
            identifier=customer_user.email,
            code=otp_code
        ).first()
        assert otp.used is True


@pytest.mark.django_db
class TestRegisterMerchantView:
    """Test /api/accounts/register-merchant/ endpoint"""
    
    def test_register_merchant_success(self, api_client):
        """Test registering a new merchant"""
        data = {
            "username": "merchantowner",
            "email": "merchantowner@test.com",
            "password": "testpass123",
            "name": "New Restaurant",
            "address": "123 Restaurant St",
            "phone": "0987654321"
        }
        response = api_client.post("/api/accounts/register-merchant/", data)
        assert response.status_code == 201
        assert "user" in response.data
        assert "merchant" in response.data
        assert response.data["merchant"]["name"] == "New Restaurant"
        
        # Verify merchant was created
        merchant = Merchant.objects.get(name="New Restaurant")
        assert merchant.owner.email == "merchantowner@test.com"
        assert merchant.owner.profile.role == "merchant"
    
    def test_register_merchant_authenticated(self, authenticated_customer_client, customer_user):
        """Test registering merchant when already authenticated"""
        data = {
            "name": "Customer's Restaurant",
            "address": "123 Restaurant St",
            "phone": "0987654322"
        }
        response = authenticated_customer_client.post("/api/accounts/register-merchant/", data)
        assert response.status_code == 201
        assert response.data["merchant"]["name"] == "Customer's Restaurant"


@pytest.mark.django_db
class TestMyMerchantsView:
    """Test /api/accounts/my-merchants/ endpoint"""
    
    def test_my_merchants_success(self, authenticated_merchant_client, merchant_user, merchant):
        """Test getting list of user's merchants"""
        response = authenticated_merchant_client.get("/api/accounts/my-merchants/")
        assert response.status_code == 200
        assert isinstance(response.data, list)
        assert len(response.data) > 0
        assert response.data[0]["id"] == merchant.id
    
    def test_my_merchants_unauthenticated(self, api_client):
        """Test getting merchants when not authenticated"""
        response = api_client.get("/api/accounts/my-merchants/")
        assert response.status_code == 401

