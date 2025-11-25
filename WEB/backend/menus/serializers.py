from rest_framework import serializers
from .models import MenuItem, Merchant


class MenuItemSerializer(serializers.ModelSerializer):
    merchant_name = serializers.CharField(source='merchant.name', read_only=True)
    merchant_address = serializers.SerializerMethodField()
    distance_km = serializers.FloatField(read_only=True, required=False, help_text='Khoảng cách từ vị trí khách hàng (km)')
    image_url = serializers.SerializerMethodField()  # Changed to SerializerMethodField
    image = serializers.ImageField(required=False, allow_null=True, write_only=True)  # For upload - write_only để chỉ nhận khi upload
    
    def get_merchant_address(self, obj):
        """Lấy địa chỉ merchant, trả về chuỗi rỗng nếu None"""
        return obj.merchant.address if obj.merchant and obj.merchant.address else ''
    
    def get_image_url(self, obj):
        """Trả về URL ảnh: ưu tiên image field, fallback về image_url"""
        if obj.image:
            request = self.context.get('request')
            if request:
                return request.build_absolute_uri(obj.image.url)
            return obj.image.url
        return obj.image_url or None
    
    def update(self, instance, validated_data):
        """Override update để xử lý upload ảnh đúng cách - giữ nguyên ảnh cũ nếu không có ảnh mới"""
        # Lấy image và image_url từ validated_data
        image = validated_data.pop('image', None)
        image_url = validated_data.pop('image_url', None)
        
        # Cập nhật các field khác
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        
        # Xử lý ảnh:
        # - Nếu có file ảnh mới (image) => upload file và xóa image_url
        # - Nếu có URL mới (image_url) => cập nhật URL
        # - Nếu không có cả hai => giữ nguyên ảnh cũ
        if image is not None:
            # Có file ảnh mới - upload file
            instance.image = image
            # Xóa image_url khi có file mới (ưu tiên file)
            if image_url:
                instance.image_url = None
        elif image_url is not None:
            # Có URL mới - cập nhật URL (giữ nguyên file nếu có, hoặc chỉ dùng URL)
            instance.image_url = image_url
        
        instance.save()
        return instance
    
    merchant_id = serializers.IntegerField(source='merchant.id', read_only=True)
    
    class Meta:
        model = MenuItem
        fields = ['id', 'name', 'description', 'price', 'stock', 'image', 'image_url', 'merchant_id', 'merchant_name', 'merchant_address', 'distance_km', 'is_available']
        read_only_fields = ['id', 'merchant_id', 'merchant_name', 'merchant_address', 'distance_km']


class MerchantSerializer(serializers.ModelSerializer):
    distance_km = serializers.FloatField(read_only=True, required=False, help_text='Khoảng cách từ vị trí khách hàng (km)')
    
    class Meta:
        model = Merchant
        fields = ['id', 'name', 'description', 'address', 'latitude', 'longitude', 'phone', 'image_url', 'is_active', 'distance_km']
