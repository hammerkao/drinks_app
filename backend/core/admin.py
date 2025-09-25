# core/admin.py
from django.contrib import admin
from .models import Product, Category, Variant, Cart, CartItem, Order, OrderItem, Store

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

@admin.register(Cart)
class CartAdmin(admin.ModelAdmin):
    list_display = ("id", "user", "updated_at")

class CartItemInline(admin.TabularInline):
    model = CartItem
    extra = 0

class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0

@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ("id", "user", "status", "total", "created_at")
    inlines = [OrderItemInline]

@admin.register(Store)
class StoreAdmin(admin.ModelAdmin):
    list_display = ("id", "name", "status", "phone")
    list_filter = ("status",)
    search_fields = ("name", "address", "phone", "open_hours")
    ordering = ("id",)
