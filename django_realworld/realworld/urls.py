from django.urls import include, path

urlpatterns = [
    path("", include("conduit.api.urls")),
    path("", include("conduit.graphql.urls")),
]
