# core/models.py
from django.db import models
from django.utils import timezone
from django.conf import settings
from django.utils.text import slugify
from django.core.validators import MinValueValidator
from django.db import models
from django.utils import timezone


User = settings.AUTH_USER_MODEL

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


class Cart(models.Model):
    # 每個 user 一個購物車（如果你要多車制，改成 ForeignKey + unique_together 即可）
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name="cart")
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Cart#{self.pk} of user {self.user}"

class CartItem(models.Model):
    cart = models.ForeignKey(Cart, on_delete=models.CASCADE, related_name="items")
    product = models.ForeignKey(Product, on_delete=models.PROTECT, related_name="cart_items")
    qty = models.PositiveIntegerField(validators=[MinValueValidator(1)])

    class Meta:
        unique_together = ("cart", "product")  # 同一購物車同一商品只會有一行
        # 外鍵預設就會建 index，不用額外加

    def __str__(self):
        return f"{self.product} x {self.qty}"

class Order(models.Model):
    class Status(models.TextChoices):
        CREATED = "created", "Created"
        COMPLETED = "completed", "Completed"
        CANCELLED = "cancelled", "Cancelled"

    user = models.ForeignKey(User, on_delete=models.PROTECT, related_name="orders")
    status = models.CharField(max_length=10, choices=Status.choices, default=Status.CREATED)
    total = models.DecimalField(max_digits=12, decimal_places=2,
                                validators=[MinValueValidator(0)], default=0)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Order#{self.pk} ({self.status})"

class OrderItem(models.Model):
    order = models.ForeignKey(Order, on_delete=models.CASCADE, related_name="items")
    product = models.ForeignKey(Product, on_delete=models.PROTECT, related_name="order_items")
    qty = models.PositiveIntegerField(validators=[MinValueValidator(1)])
    # 下單當下的單價，與 Product.price 脫鉤
    unit_price = models.DecimalField(max_digits=10, decimal_places=2,
                                     validators=[MinValueValidator(0)])

    class Meta:
        unique_together = ("order", "product")

    def __str__(self):
        return f"{self.product} x {self.qty} @ {self.unit_price}"

class Store(models.Model):
    name = models.CharField(max_length=100)
    phone = models.CharField(max_length=20, blank=True)
    address = models.CharField(max_length=200, blank=True)
    open_hours = models.CharField(max_length=100, blank=True)  # "09:00 - 22:00"
    status = models.CharField(max_length=20, blank=True)       # "營業中/休息中"

    def __str__(self): return self.name

 