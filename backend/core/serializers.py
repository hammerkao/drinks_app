# core/serializers.py
from rest_framework import serializers
from .models import Product, Category, Variant

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
