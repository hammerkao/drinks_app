# core/views.py
from rest_framework import viewsets, permissions, filters as drf_filters
from django_filters.rest_framework import DjangoFilterBackend
from django_filters import rest_framework as filters

from .models import Product, Category, Variant
from .serializers import ProductSerializer, CategorySerializer, VariantSerializer


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
