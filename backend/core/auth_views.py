from django.contrib.auth.models import User
from django.contrib.auth import authenticate
from rest_framework import serializers, status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.permissions import AllowAny
from rest_framework_simplejwt.tokens import RefreshToken

# === Serializers ===

class RegisterSerializer(serializers.Serializer):
    phone = serializers.RegexField(regex=r'^\d{9,15}$', max_length=15)
    password = serializers.CharField(min_length=6, write_only=True)

    def validate_phone(self, value):
        # 用 username 存手機；若你自訂了 User.phone，這裡改查 phone 欄位即可
        if User.objects.filter(username=value).exists():
            raise serializers.ValidationError("此手機已被註冊")
        return value

    def create(self, validated_data):
        user = User.objects.create_user(
            username=validated_data["phone"],
            password=validated_data["password"]
        )
        return user


class LoginSerializer(serializers.Serializer):
    phone = serializers.CharField()
    password = serializers.CharField()


# === Views ===

class RegisterView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        ser = RegisterSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        user = ser.save()
        return Response({"id": user.id, "phone": user.username}, status=status.HTTP_201_CREATED)


class LoginView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        ser = LoginSerializer(data=request.data)
        ser.is_valid(raise_exception=True)

        phone = ser.validated_data["phone"]
        password = ser.validated_data["password"]

        user = authenticate(request, username=phone, password=password)
        if not user:
            return Response({"detail": "手機或密碼不正確"}, status=status.HTTP_400_BAD_REQUEST)

        refresh = RefreshToken.for_user(user)
        access_token = str(refresh.access_token)
        return Response({"token": access_token}, status=status.HTTP_200_OK)
