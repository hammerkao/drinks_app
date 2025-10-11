# core/views.py
from decimal import Decimal
from django.db import transaction
from django.shortcuts import get_object_or_404

from rest_framework import viewsets, permissions, status, filters as drf_filters
from rest_framework.decorators import action
from rest_framework.response import Response

from django_filters.rest_framework import DjangoFilterBackend
from django_filters import rest_framework as filters

from django.utils.dateparse import parse_datetime
from django.utils import timezone

from .models import (
    Category, Product, Variant,
    Cart, CartItem,
    Order, OrderItem,
    Store,
)
from .serializers import (
    CategorySerializer, ProductSerializer, VariantSerializer,
    CartSerializer, OrderSerializer, StoreSerializer,
    AddCartItemSerializer,
)
from rest_framework.filters import SearchFilter, OrderingFilter


# ===== 共用：把客製選項轉成 key（避免同品項不同客製被合併） =====
def build_options_key(sweet, ice, toppings, note):
    tops = ",".join(sorted((toppings or [])))
    return f"{sweet or ''}|{ice or ''}|{tops}|{note or ''}"


# ---------- Store ----------
class StoreViewSet(viewsets.ModelViewSet):
    queryset = Store.objects.all().order_by("id")
    serializer_class = StoreSerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly]
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    filterset_fields = ["status"]
    search_fields = ["name", "address", "phone", "open_hours"]
    ordering_fields = ["id", "name"]


# ---------- Category ----------
class CategoryViewSet(viewsets.ModelViewSet):
    queryset = Category.objects.all().order_by("order", "id")
    serializer_class = CategorySerializer
    permission_classes = [permissions.IsAuthenticatedOrReadOnly]
    filter_backends = [DjangoFilterBackend, drf_filters.SearchFilter, drf_filters.OrderingFilter]
    search_fields = ["name", "slug"]
    ordering_fields = ["order", "name", "id"]


# ---------- Product ----------
class ProductFilter(filters.FilterSet):
    min_price = filters.NumberFilter(field_name="price", lookup_expr="gte")
    max_price = filters.NumberFilter(field_name="price", lookup_expr="lte")
    category  = filters.NumberFilter(field_name="category_id")

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


# ---------- Variant ----------
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


# ---------- Cart ----------
class CartViewSet(viewsets.GenericViewSet):
    """
    一人一車；/carts/me/ 取得或建立購物車；add/update/remove/clear。
    合併邏輯使用 product + options_key。
    """
    serializer_class = CartSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        qs = Cart.objects.select_related("user").prefetch_related("items__product")
        return qs if self.request.user.is_staff else qs.filter(user=self.request.user)

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
        ser = AddCartItemSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        data = ser.validated_data

        product = get_object_or_404(Product, pk=data["product_id"], is_active=True)
        qty = int(data.get("qty", 1))
        if qty <= 0:
            return Response({"detail": "qty 必須 > 0"}, status=400)

        sweet   = data.get("sweet")
        ice     = data.get("ice")
        toppings= data.get("toppings") or []
        note    = data.get("note")
        options_price = Decimal(str(data.get("options_price", 0)))

        options_key = build_options_key(sweet, ice, toppings, note)

        item, created = CartItem.objects.get_or_create(
            cart=cart, product=product, options_key=options_key,
            defaults={
                "qty": qty,
                "sweet": sweet,
                "ice": ice,
                "toppings_json": toppings,
                "note": note,
                "options_price": options_price,
            }
        )
        if not created:
            item.qty += qty
            item.sweet = sweet
            item.ice = ice
            item.toppings_json = toppings
            item.note = note
            item.options_price = options_price
            item.save()

        return Response({"detail": "OK"}, status=201)

    @action(detail=False, methods=["patch"], url_path="update_item")
    def update_item(self, request):
        cart = self.get_object()
        product_id  = request.data.get("product_id")
        options_key = request.data.get("options_key", "")
        qty = int(request.data.get("qty", 1))
        item = get_object_or_404(CartItem, cart=cart, product_id=product_id, options_key=options_key)
        if qty <= 0:
            item.delete()
        else:
            item.qty = qty
            item.save()
        return Response({"detail": "OK"})

    @action(detail=False, methods=["post"], url_path="remove_item")
    def remove_item(self, request):
        cart = self.get_object()
        product_id  = request.data.get("product_id")
        options_key = request.data.get("options_key", "")
        get_object_or_404(CartItem, cart=cart, product_id=product_id, options_key=options_key).delete()
        return Response({"detail": "OK"})

    @action(detail=False, methods=["delete"], url_path="clear")
    def clear(self, request):
        cart = self.get_object()
        cart.items.all().delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


# ---------- Order ----------
class OrderViewSet(viewsets.ModelViewSet):
    """
    建立訂單：
      - 若 request.data 有 items（product_id/qty/options_price...），直接用 items 建單
      - 否則從目前購物車生成
    金額計算：(product.price + options_price) * qty
    """
    serializer_class = OrderSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        qs = (
            Order.objects
            .select_related("user", "store")
            .prefetch_related("items__product")
            .order_by("-id")
        )
        return qs if self.request.user.is_staff else qs.filter(user=self.request.user)

    @transaction.atomic
    def create(self, request, *args, **kwargs):
        data = request.data

        # 解析分店
        store_id = data.get("store_id") or data.get("store")
        store = None
        if store_id:
            try:
                store = Store.objects.get(pk=int(store_id))
            except (ValueError, Store.DoesNotExist):
                return Response({"detail": "store_id 無效"}, status=400)

        # ★ 接住訂單備註（整張訂單）
        buyer_name     = (data.get("buyer_name") or "").strip() or None
        buyer_phone    = (data.get("buyer_phone") or "").strip() or None
        payment_method = (data.get("payment_method") or "").strip() or None   # e.g. "cash"
        pickup_method  = (data.get("pickup_method") or "").strip() or None    # e.g. "pickup"
        order_note     = (data.get("order_note") or "").strip() or None

         # pickup_time 轉成 aware datetime
        pickup_time = None
        if data.get("pickup_time"):
            dt = parse_datetime(data["pickup_time"])
            if dt is not None and timezone.is_naive(dt):
                dt = timezone.make_aware(dt, timezone.get_current_timezone())
            pickup_time = dt

        # ★ 只建立一次，後面兩種流程都重用這個 order
        order = Order.objects.create(
            user=request.user,
            store=store,
            buyer_name=buyer_name,
            buyer_phone=buyer_phone,
            payment_method=payment_method,
            pickup_method=pickup_method,
            pickup_time=pickup_time,
            order_note=order_note,
        )

        items_data = data.get("items")
        if items_data:
            # ===== 方法 B：payload items 建單 =====
            total = Decimal("0.00")
            bulk = []
            for it in items_data:
                product = get_object_or_404(Product, pk=it.get("product_id"), is_active=True)
                qty = int(it.get("qty", 1))
                unit_price    = Decimal(str(it.get("unit_price", product.price)))
                options_price = Decimal(str(it.get("options_price", 0)))
                sweet   = it.get("sweet") or None
                ice     = it.get("ice") or None
                toppings= it.get("toppings") or []
                note    = it.get("note") or None
                options_key = it.get("options_key") or build_options_key(sweet, ice, toppings, note)

                total += (unit_price + options_price) * qty
                bulk.append(OrderItem(
                    order=order, product=product, qty=qty,
                    unit_price=unit_price, options_price=options_price,
                    sweet=sweet, ice=ice, toppings_json=toppings,
                    note=note, options_key=options_key,
                ))
            OrderItem.objects.bulk_create(bulk)
            order.total = total
            order.save(update_fields=["total"])  # 其他欄位已在 create() 寫入
            ser = self.get_serializer(order)
            return Response(ser.data, status=status.HTTP_201_CREATED)

        # ===== 方法 A：從購物車轉單 =====
        cart, _ = Cart.objects.get_or_create(user=request.user)
        cart_items = list(cart.items.select_related("product"))
        if not cart_items:
            return Response({"detail": "購物車是空的"}, status=400)

        total = Decimal("0.00")
        bulk = []
        for ci in cart_items:
            base_price = ci.product.price
            add_price  = ci.options_price or Decimal("0.00")
            total += (base_price + add_price) * ci.qty
            bulk.append(OrderItem(
                order=order, product=ci.product, qty=ci.qty,
                unit_price=base_price, options_price=add_price,
                sweet=ci.sweet, ice=ci.ice, toppings_json=ci.toppings_json,
                note=ci.note, options_key=ci.options_key,
            ))
        OrderItem.objects.bulk_create(bulk)
        order.total = total
        order.save(update_fields=["total"])
        cart.items.all().delete()

        ser = self.get_serializer(order)
        return Response(ser.data, status=status.HTTP_201_CREATED)
