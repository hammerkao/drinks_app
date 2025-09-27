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
    image_url = serializers.SerializerMethodField()

    class Meta:
        model = Product
        fields = [
            "id",
            "name",
            "price",
            "is_active",
            "created_at",
            "updated_at",
            "image_url",
        ]

    def get_image_url(self, obj):
        request = self.context.get("request")
        # 若 model 有 image 欄位就回傳；沒有就回 None
        if hasattr(obj, "image") and getattr(obj, "image"):
            url = getattr(obj.image, "url", None)
            if url:
                return request.build_absolute_uri(url) if request else url
        return None


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

