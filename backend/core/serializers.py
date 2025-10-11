# core/serializers.py
from rest_framework import serializers
from .models import (
    Product, Category, Variant,
    Cart, CartItem,
    Order, OrderItem,
    Store,
)



# ---------- Category ----------
class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = Category
        fields = ["id", "name", "slug", "order"]


# ---------- Variant ----------
class VariantSerializer(serializers.ModelSerializer):
    class Meta:
        model = Variant
        fields = ["id", "product", "name", "price", "is_active"]
        read_only_fields = ["id"]


# ---------- Product ----------
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
        if hasattr(obj, "image") and getattr(obj, "image"):
            url = getattr(obj.image, "url", None)
            if url:
                return request.build_absolute_uri(url) if request else url
        return None


# ---------- Cart ----------
class CartItemSerializer(serializers.ModelSerializer):
    # 寫入時送 product id
    product = serializers.PrimaryKeyRelatedField(
        queryset=Product.objects.all(), write_only=True
    )
    # 讀取時回傳基本資訊
    product_detail = serializers.SerializerMethodField(read_only=True)

    class Meta:
        model = CartItem
        fields = [
            "id", "product", "product_detail", "qty",
            "sweet", "ice", "toppings_json", "note",
            "options_price", "options_key",
        ]
        read_only_fields = ["id", "product_detail", "options_key"]

    def get_product_detail(self, obj):
        return {
            "id": obj.product_id,
            "name": obj.product.name,
            "price": str(obj.product.price),
        }


class CartSerializer(serializers.ModelSerializer):
    items = CartItemSerializer(many=True, read_only=True)

    class Meta:
        model = Cart
        fields = ["id", "user", "updated_at", "items"]
        read_only_fields = ["id", "user", "updated_at", "items"]


# ---------- Order ----------
class OrderItemSerializer(serializers.ModelSerializer):
    product_detail = serializers.SerializerMethodField(read_only=True)
    toppings = serializers.JSONField(source="toppings_json", read_only=True)

    class Meta:
        model = OrderItem
        fields = [
            "id", "product", "product_detail",
            "qty", "unit_price", "options_price",
            "sweet", "ice", "toppings", "note", "options_key",
        ]

    def get_product_detail(self, obj):
        return {"id": obj.product_id, "name": obj.product.name}


class OrderSerializer(serializers.ModelSerializer):
    items = OrderItemSerializer(many=True, read_only=True)
    store_name = serializers.CharField(source="store.name", read_only=True)  # 若模型已加 store FK

    class Meta:
        model = Order
        fields = ["id", "user", "status", "total", "created_at", "store", "store_name",
                    "buyer_name", "buyer_phone",
                    "payment_method", "paid",
                    "pickup_method", "pickup_time",
                    "items"]
        read_only_fields = ["id", "user", "status", "total", "created_at", "items"]



# ---------- Store ----------
class StoreSerializer(serializers.ModelSerializer):
    class Meta:
        model = Store
        fields = ["id", "name", "phone", "address", "open_hours", "status"]


# ---------- carts/add_item/ 用 ----------
class AddCartItemSerializer(serializers.Serializer):
    product_id    = serializers.IntegerField()
    qty           = serializers.IntegerField(min_value=1, default=1)
    sweet         = serializers.CharField(allow_blank=True, required=False, allow_null=True)
    ice           = serializers.CharField(allow_blank=True, required=False, allow_null=True)
    toppings      = serializers.ListField(
        child=serializers.CharField(), required=False, allow_null=True
    )
    note          = serializers.CharField(allow_blank=True, required=False, allow_null=True)
    # 後端 models 是 DecimalField → 這裡用 DecimalField 對齊
    options_price = serializers.DecimalField(max_digits=10, decimal_places=2, required=False, default=0)

    def validate(self, attrs):
        # 可加白名單檢核（例如 sweet/ice choices）
        return attrs
