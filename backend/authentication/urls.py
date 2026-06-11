from django.urls import path

from authentication.views import (
    CurrentUserAPIView,
    LoginAPIView,
    RegistrationAPIView,
)

urlpatterns = [
    path("users", RegistrationAPIView.as_view()),
    path("users/login", LoginAPIView.as_view()),
    path("user", CurrentUserAPIView.as_view()),
]
