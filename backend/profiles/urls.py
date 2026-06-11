from django.urls import path

from profiles.views import ProfileFollowAPIView, ProfileRetrieveAPIView

urlpatterns = [
    path("profiles/<str:username>", ProfileRetrieveAPIView.as_view()),
    path("profiles/<str:username>/follow", ProfileFollowAPIView.as_view()),
]
