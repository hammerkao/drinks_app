# core/models.py
from django.db import models
from django.utils import timezone
from django.utils.text import slugify

class Category(models.Model):
    name  = models.CharField(max_length=50, unique=True)
    slug  = models.SlugField(unique=True)
    order = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["order", "id"]

    def __str__(self):
        return self.name

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = slugify(self.name)
        super().save(*args, **kwargs)


class Product(models.Model):
    name = models.CharField(max_length=100, db_index=True)
    price = models.DecimalField(max_digits=10, decimal_places=2, db_index=True)
    is_active = models.BooleanField(default=True, db_index=True)
    created_at = models.DateTimeField(default=timezone.now)  # 用 default 避免遷移詢問
    updated_at = models.DateTimeField(auto_now=True)

    # 階段2新增欄位（可先留空）
    category = models.ForeignKey(
        Category, on_delete=models.PROTECT,
        null=True, blank=True, related_name="products"
    )
    image = models.ImageField(upload_to="products/", null=True, blank=True)

    def __str__(self):
        return self.name


class Variant(models.Model):
    product = models.ForeignKey(Product, on_delete=models.CASCADE, related_name="variants")
    name    = models.CharField(max_length=30)  # 例如 S / M / L
    price   = models.DecimalField(max_digits=10, decimal_places=2)
    is_active = models.BooleanField(default=True)

    class Meta:
        unique_together = [("product", "name")]
        ordering = ["product_id", "price", "id"]

    def __str__(self):
        return f"{self.product.name} - {self.name}"
