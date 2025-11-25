from rest_framework import serializers
from .models import Order, OrderItem
from menus.models import MenuItem
from django.db import transaction


class OrderItemSerializer(serializers.ModelSerializer):
    class Meta:
        model = OrderItem
        fields = ['menu_item_id', 'quantity']


class OrderSerializer(serializers.ModelSerializer):
    items = OrderItemSerializer(many=True)

    class Meta:
        model = Order
        fields = ['id', 'created_at', 'items']

    def create(self, validated_data):
        items_data = validated_data.pop('items', [])
        # Transactionally create order and decrement stock
        with transaction.atomic():
            order = Order.objects.create(**validated_data)
            for item in items_data:
                menu_id = item['menu_item_id']
                qty = item.get('quantity', 1)
                menu = MenuItem.objects.select_for_update().get(pk=menu_id)
                if menu.stock < qty:
                    raise serializers.ValidationError(f'Not enough stock for {menu.name}')
                menu.stock -= qty
                menu.save()
                OrderItem.objects.create(order=order, menu_item_id=menu_id, quantity=qty)
        return order
