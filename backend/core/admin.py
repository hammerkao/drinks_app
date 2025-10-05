# core/admin.py
from django.contrib import admin
from django import forms
from django.db import models  
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
    # 精簡欄位、調整順序
    fields = (
        "product", "qty", "unit_price", "options_price",
        "sweet", "ice", "toppings_json", "note",
    )
    # 產品用自動完成，比較省空間
    autocomplete_fields = ("product",)

    # 把欄位 widget 都縮窄
    formfield_overrides = {
        models.PositiveIntegerField: {
            "widget": forms.NumberInput(attrs={"style": "width:70px"})
        },
        models.DecimalField: {
            "widget": forms.NumberInput(attrs={"style": "width:90px"})
        },
        models.CharField: {
            "widget": forms.TextInput(attrs={"style": "width:130px"})
        },
        models.JSONField: {
            "widget": forms.Textarea(attrs={
                "rows": 1,
                "style": "height:28px;width:200px;white-space:nowrap;overflow:auto;"
            })
        },
        models.TextField: {
            "widget": forms.Textarea(attrs={
                "rows": 1,
                "style": "height:28px;width:180px;white-space:nowrap;overflow:auto;"
            })
        },
    }

@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ("id", "user", "store", "status", "total", "created_at")
    list_filter  = ("status", "store")
    search_fields = ("user__username", "user__phone", "store__name")
    readonly_fields = ("total", "created_at")
    inlines = [OrderItemInline]


@admin.register(Store)
class StoreAdmin(admin.ModelAdmin):
    list_display = ("id", "name", "status", "phone")
    list_filter = ("status",)
    search_fields = ("name", "address", "phone", "open_hours")
    ordering = ("id",)

@admin.register(CartItem)
class CartItemAdmin(admin.ModelAdmin):
    list_display = ("id", "cart", "product", "qty", "sweet", "ice", "options_price", "options_key")
    search_fields = ("options_key", "note")
    list_filter = ("sweet", "ice")

@admin.register(OrderItem)
class OrderItemAdmin(admin.ModelAdmin):
    list_display = ("id", "order", "product", "qty", "unit_price", "options_price", "sweet", "ice", "options_key")
    search_fields = ("options_key", "note")
    list_filter = ("sweet", "ice")
    readonly_fields = ("line_total",)

    @admin.display(description="Line Total")
    def line_total(self, obj):
        return (obj.unit_price + obj.options_price) * obj.qty


