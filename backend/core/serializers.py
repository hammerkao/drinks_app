# core/serializers.py
from rest_framework import serializers
from .models import Product, Category, Variant, Cart, CartItem, Order, OrderItem, Store

class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = Category
        fields = ["id", "name", "slug", "order"]

class VariantSerializer(serializers.ModelSerializer):
    class Meta:
        model = Variant
        fields = ["id", "product", "name", "price", "is_active"]
        read_only_fields = ["id"]

class ProductSerializer(serializers.ModelSerializer):
    category = serializers.PrimaryKeyRelatedField(
        queryset=Category.objects.all(), allow_null=True, required=False
    )
    category_name = serializers.SerializerMethodField()
    image = serializers.ImageField(required=False, allow_null=True)
    variants = VariantSerializer(many=True, read_only=True)

    class Meta:
        model = Product
        fields = [
            "id", "name", "price", "is_active",
            "created_at", "updated_at",
            "category", "category_name", "image",
            "variants",
        ]
        read_only_fields = ["id", "created_at", "updated_at", "variants", "category_name"]

    def get_category_name(self, obj):
        return obj.category.name if obj.category else None

    def validate_price(self, value):
        if value < 0:
            raise serializers.ValidationError("Price must be >= 0.")
        return value


class CartItemSerializer(serializers.ModelSerializer):
    product = serializers.PrimaryKeyRelatedField(queryset=Product.objects.all(), write_only=True)
    product_detail = serializers.SerializerMethodField(read_only=True)

    class Meta:
        model = CartItem
        fields = ["id", "product", "product_detail", "qty"]

    def get_product_detail(self, obj):
        return {"id": obj.product_id, "name": obj.product.name, "price": str(obj.product.price)}

class CartSerializer(serializers.ModelSerializer):
    items = CartItemSerializer(many=True, read_only=True)

    class Meta:
        model = Cart
        fields = ["id", "user", "updated_at", "items"]
        read_only_fields = ["id", "user", "updated_at", "items"]

class OrderItemSerializer(serializers.ModelSerializer):
    product_detail = serializers.SerializerMethodField(read_only=True)

    class Meta:
        model = OrderItem
        fields = ["id", "product", "product_detail", "qty", "unit_price"]

    def get_product_detail(self, obj):
        return {"id": obj.product_id, "name": obj.product.name}

class OrderSerializer(serializers.ModelSerializer):
    items = OrderItemSerializer(many=True, read_only=True)

    class Meta:
        model = Order
        fields = ["id", "user", "status", "total", "created_at", "items"]
        read_only_fields = ["id", "user", "status", "total", "created_at", "items"]

class StoreSerializer(serializers.ModelSerializer):
    class Meta:
        model = Store
        fields = ["id", "name", "phone", "address", "open_hours", "status"]

