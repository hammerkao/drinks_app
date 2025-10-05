# core/models.py
from django.db import models
from django.utils import timezone
from django.conf import settings
from django.utils.text import slugify
from django.core.validators import MinValueValidator

User = settings.AUTH_USER_MODEL


# -------- Category --------
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


# -------- Product --------
class Product(models.Model):
    name = models.CharField(max_length=100, db_index=True)
    price = models.DecimalField(max_digits=10, decimal_places=2, db_index=True)
    is_active = models.BooleanField(default=True, db_index=True)
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(auto_now=True)

    category = models.ForeignKey(
        Category, on_delete=models.PROTECT,
        null=True, blank=True, related_name="products"
    )
    image = models.ImageField(upload_to="products/", null=True, blank=True)

    def __str__(self):
        return self.name


# -------- Variant（可選）--------
class Variant(models.Model):
    product = models.ForeignKey(Product, on_delete=models.CASCADE, related_name="variants")
    name    = models.CharField(max_length=30)  # e.g. S / M / L
    price   = models.DecimalField(max_digits=10, decimal_places=2)
    is_active = models.BooleanField(default=True)

    class Meta:
        unique_together = [("product", "name")]
        ordering = ["product_id", "price", "id"]

    def __str__(self):
        return f"{self.product.name} - {self.name}"


# -------- Store --------
class Store(models.Model):
    name = models.CharField(max_length=100)
    phone = models.CharField(max_length=20, blank=True)
    address = models.CharField(max_length=200, blank=True)
    open_hours = models.CharField(max_length=100, blank=True)
    status = models.CharField(max_length=20, blank=True)  # "營業中/休息中"

    def __str__(self):
        return self.name


# -------- Cart / CartItem --------
class Cart(models.Model):
    # 一個 user 一台購物車（簡化）
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name="cart")
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Cart#{self.pk} of user {self.user}"


class CartItem(models.Model):
    cart    = models.ForeignKey(Cart, on_delete=models.CASCADE, related_name="items")
    # 重要：related_name 應為 cart_items，且用字串引用避免解析順序問題
    product = models.ForeignKey("core.Product", on_delete=models.PROTECT, related_name="cart_items")
    qty     = models.PositiveIntegerField(validators=[MinValueValidator(1)])

    # 客製欄位
    sweet   = models.CharField(max_length=20, blank=True, null=True)
    ice     = models.CharField(max_length=20, blank=True, null=True)
    toppings_json = models.JSONField(blank=True, null=True)  # Django 3.1+；不行就改 TextField
    note          = models.CharField(max_length=200, blank=True, null=True)
    options_price = models.DecimalField(max_digits=10, decimal_places=2, default=0)

    # 用於分辨同商品不同客製
    options_key   = models.CharField(max_length=255, db_index=True, default="")

    class Meta:
        unique_together = ("cart", "product", "options_key")

    def __str__(self):
        return f"{self.product} x {self.qty}"


# -------- Order / OrderItem --------
class Order(models.Model):
    class Status(models.TextChoices):
        CREATED   = "created", "Created"
        COMPLETED = "completed", "Completed"
        CANCELLED = "cancelled", "Cancelled"

    user = models.ForeignKey(User, on_delete=models.PROTECT, related_name="orders")
    # ✅ 新增：關聯分店（可為空，方便舊資料遷移）
    store = models.ForeignKey("core.Store", on_delete=models.PROTECT,
                              related_name="orders", null=True, blank=True)

    status = models.CharField(max_length=10, choices=Status.choices, default=Status.CREATED)
    total  = models.DecimalField(max_digits=12, decimal_places=2,
                                 validators=[MinValueValidator(0)], default=0)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Order#{self.pk} ({self.status})"


class OrderItem(models.Model):
    order   = models.ForeignKey(Order, on_delete=models.CASCADE, related_name="items")
    # 用字串引用，且與 CartItem 不同的 related_name
    product = models.ForeignKey("core.Product", on_delete=models.PROTECT, related_name="order_items")
    qty     = models.PositiveIntegerField(validators=[MinValueValidator(1)])

    # 下單當下的商品單價（不含加價）
    unit_price    = models.DecimalField(max_digits=10, decimal_places=2,
                                        validators=[MinValueValidator(0)])

    # 將客製資訊帶入訂單
    sweet   = models.CharField(max_length=20, blank=True, null=True)
    ice     = models.CharField(max_length=20, blank=True, null=True)
    toppings_json = models.JSONField(blank=True, null=True)  # 或 TextField
    note          = models.CharField(max_length=200, blank=True, null=True)
    options_price = models.DecimalField(max_digits=10, decimal_places=2, default=0)
    options_key   = models.CharField(max_length=255, db_index=True, default="")

    class Meta:
        unique_together = ("order", "product", "options_key")

    def __str__(self):
        return f"{self.product} x {self.qty} @ {self.unit_price} (+{self.options_price})"
