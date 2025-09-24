from django.urls import path
from .auth_views import RegisterView, LoginView

urlpatterns = [
    path("register/", RegisterView.as_view(), name="auth-register"),
    path("login/", LoginView.as_view(), name="auth-login"),
]
