from rest_framework.permissions import BasePermission, SAFE_METHODS

def get_role(user) -> str:
    try:
        return getattr(user.profile, "role", "customer")
    except Exception:
        return "customer"

class IsMerchant(BasePermission):
    def has_permission(self, request, view):
        u = request.user
        return bool(u and u.is_authenticated and get_role(u) in ("merchant", "admin"))

class IsMerchantOrReadOnly(BasePermission):
    def has_permission(self, request, view):
        if request.method in SAFE_METHODS:
            return True
        u = request.user
        return bool(u and u.is_authenticated and get_role(u) in ("merchant", "admin"))

class IsShipper(BasePermission):
    def has_permission(self, request, view):
        u = request.user
        return bool(u and u.is_authenticated and get_role(u) == "shipper")

class IsCustomer(BasePermission):
    def has_permission(self, request, view):
        u = request.user
        return bool(u and u.is_authenticated and get_role(u) == "customer")
