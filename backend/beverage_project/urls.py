"""
URL configuration for beverage_project project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/5.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
# beverage_project/urls.py
from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from core.views import ProductViewSet, CategoryViewSet, VariantViewSet, CartViewSet, OrderViewSet, StoreViewSet

router = DefaultRouter()
router.register(r"products", ProductViewSet, basename="product")
router.register(r"categories", CategoryViewSet, basename="category")
router.register(r"variants", VariantViewSet, basename="variant")
router.register(r"carts", CartViewSet, basename="cart")
router.register(r"orders", OrderViewSet, basename="order")
router.register(r"stores", StoreViewSet, basename="stores")



urlpatterns = [
    path('admin/', admin.site.urls),
    path("api/auth/", include("core.auth_urls")),   # ⬅ 新增這行
    
     # ✅ 開文件用：Swagger UI 會去抓這個 schema
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),
    # ✅ 文件頁面本身
    path("api/docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),

    # 🔐（可選）JWT：在 Swagger 的「Authorize」輸入 Bearer token 會用得到
    path("api/auth/token/", TokenObtainPairView.as_view(), name="token_obtain_pair"),
    path("api/auth/token/refresh/", TokenRefreshView.as_view(), name="token_refresh"),
]
