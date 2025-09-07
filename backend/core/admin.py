# core/admin.py
from django.contrib import admin
from .models import Product, Category, Variant

@admin.register(Category)
class CategoryAdmin(admin.ModelAdmin):
    list_display = ("id", "name", "slug", "order")
    search_fields = ("name", "slug")
    ordering = ("order", "id")

@admin.register(Product)
class ProductAdmin(admin.ModelAdmin):
    list_display = ("id", "name", "price", "is_active", "category", "created_at")
    list_filter = ("is_active", "category")
    search_fields = ("name",)

@admin.register(Variant)
class VariantAdmin(admin.ModelAdmin):
    list_display = ("id", "product", "name", "price", "is_active")
    list_filter = ("is_active", "product")
    search_fields = ("name", "product__name")
