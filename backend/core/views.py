# core/views.py
from decimal import Decimal
from django.db import transaction
from django.shortcuts import get_object_or_404

from rest_framework import viewsets, permissions, status, filters as drf_filters
from rest_framework.viewsets import ReadOnlyModelViewSet
from rest_framework.permissions import IsAuthenticated
from rest_framework.decorators import action
from rest_framework.response import Response

from django_filters.rest_framework import DjangoFilterBackend
from django_filters import rest_framework as filters

from .models import Category, Product, Variant, Cart, CartItem, Order, OrderItem, Store
from .serializers import (
    CategorySerializer, ProductSerializer, VariantSerializer,
    CartSerializer, OrderSerializer, StoreSerializer
)


from rest_framework.filters import SearchFilter, OrderingFilter




class StoreViewSet(viewsets.ModelViewSet):
    queryset = Store.objects.all().order_by("id")
    serializer_class = StoreSerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly]
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    # 依你的欄位調整（open_hours 通常不用篩選；status 常用來啟用/停用）
    filterset_fields = ["status"]
    search_fields = ["name", "address", "phone", "open_hours"]
    ordering_fields = ["id", "name"]


# ---- Category ----
class CategoryViewSet(viewsets.ModelViewSet):
    queryset = Category.objects.all().order_by("order", "id")
    serializer_class = CategorySerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly]
    filter_backends = [DjangoFilterBackend, drf_filters.SearchFilter, drf_filters.OrderingFilter]
    search_fields = ["name", "slug"]
    ordering_fields = ["order", "name", "id"]


# ---- Product ----
class ProductFilter(filters.FilterSet):
    min_price = filters.NumberFilter(field_name="price", lookup_expr="gte")
    max_price = filters.NumberFilter(field_name="price", lookup_expr="lte")
    category = filters.NumberFilter(field_name="category_id")

    class Meta:
        model = Product
        fields = ["is_active", "min_price", "max_price", "category"]

class ProductViewSet(viewsets.ModelViewSet):
    queryset = (
        Product.objects
        .select_related("category")
        .prefetch_related("variants")
        .order_by("id")
    )
    serializer_class = ProductSerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly]
    filter_backends = [DjangoFilterBackend, drf_filters.SearchFilter, drf_filters.OrderingFilter]
    filterset_class = ProductFilter
    search_fields = ["name"]
    ordering_fields = ["price", "name", "created_at"]
    ordering = ["id"]


# ---- Variant ----
class VariantFilter(filters.FilterSet):
    min_price = filters.NumberFilter(field_name="price", lookup_expr="gte")
    max_price = filters.NumberFilter(field_name="price", lookup_expr="lte")

    class Meta:
        model = Variant
        fields = ["product", "is_active", "min_price", "max_price"]

class VariantViewSet(viewsets.ModelViewSet):
    queryset = Variant.objects.select_related("product").order_by("product_id", "price", "id")
    serializer_class = VariantSerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly]
    filter_backends = [DjangoFilterBackend, drf_filters.SearchFilter, drf_filters.OrderingFilter]
    filterset_class = VariantFilter
    search_fields = ["name"]
    ordering_fields = ["price", "name", "id"]




class IsOwnerOrAdmin(permissions.BasePermission):
    def has_object_permission(self, request, view, obj):
        user = getattr(obj, "user", None)
        return request.user and (request.user.is_staff or user == request.user)

class CartViewSet(viewsets.GenericViewSet):
    """
    一人一車；提供 /carts/me/ 取得或建立購物車，以及增刪改項目。
    """
    serializer_class = CartSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        qs = Cart.objects.select_related("user").prefetch_related("items__product")
        if self.request.user.is_staff:
            return qs
        return qs.filter(user=self.request.user)

    def get_object(self):
        cart, _ = Cart.objects.get_or_create(user=self.request.user)
        self.check_object_permissions(self.request, cart)
        return cart

    @action(detail=False, methods=["get"], url_path="me")
    def me(self, request):
        cart = self.get_object()
        return Response(self.get_serializer(cart).data)

    @action(detail=False, methods=["post"], url_path="add_item")
    def add_item(self, request):
        cart = self.get_object()
        product_id = request.data.get("product_id")
        qty = int(request.data.get("qty", 1))
        if qty <= 0:
            return Response({"detail": "qty 必須 > 0"}, status=400)
        product = get_object_or_404(Product, pk=product_id, is_active=True)
        item, created = CartItem.objects.get_or_create(cart=cart, product=product, defaults={"qty": qty})
        if not created:
            item.qty += qty
            item.save()
        return Response({"detail": "OK"})

    @action(detail=False, methods=["patch"], url_path="update_item")
    def update_item(self, request):
        cart = self.get_object()
        product_id = request.data.get("product_id")
        qty = int(request.data.get("qty", 1))
        item = get_object_or_404(CartItem, cart=cart, product_id=product_id)
        if qty <= 0:
            item.delete()
        else:
            item.qty = qty
            item.save()
        return Response({"detail": "OK"})

    @action(detail=False, methods=["post"], url_path="remove_item")
    def remove_item(self, request):
        cart = self.get_object()
        product_id = request.data.get("product_id")
        get_object_or_404(CartItem, cart=cart, product_id=product_id).delete()
        return Response({"detail": "OK"})

    @action(detail=False, methods=["delete"], url_path="clear")
    def clear(self, request):
        cart = self.get_object()
        cart.items.all().delete()
        return Response(status=status.HTTP_204_NO_CONTENT)

class OrderViewSet(viewsets.ModelViewSet):
    """
    列出/查看自己的訂單，建立訂單會從目前購物車產生 Order 與 OrderItem。
    """
    serializer_class = OrderSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        qs = Order.objects.select_related("user").prefetch_related("items__product").order_by("-id")
        if self.request.user.is_staff:
            return qs
        return qs.filter(user=self.request.user)

    @transaction.atomic
    def create(self, request, *args, **kwargs):
        # 從購物車生成訂單
        cart, _ = Cart.objects.get_or_create(user=request.user)
        items = list(cart.items.select_related("product"))
        if not items:
            return Response({"detail": "購物車是空的"}, status=400)

        order = Order.objects.create(user=request.user)  # status 預設 created
        total = Decimal("0.00")
        bulk = []
        for ci in items:
            unit_price = ci.product.price
            total += unit_price * ci.qty
            bulk.append(OrderItem(order=order, product=ci.product, qty=ci.qty, unit_price=unit_price))
        OrderItem.objects.bulk_create(bulk)
        order.total = total
        order.save()

        cart.items.all().delete()  # 清空購物車
        serializer = self.get_serializer(order)
        headers = self.get_success_headers(serializer.data)
        return Response(serializer.data, status=status.HTTP_201_CREATED, headers=headers)
