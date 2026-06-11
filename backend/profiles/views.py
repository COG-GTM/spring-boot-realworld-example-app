from rest_framework.generics import get_object_or_404
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from authentication.models import User
from profiles.models import Follow
from profiles.serializers import ProfileSerializer


def _profile_response(target, request):
    return Response({"profile": ProfileSerializer(target, context={"request": request}).data})


class ProfileRetrieveAPIView(APIView):
    permission_classes = [AllowAny]

    def get(self, request, username):
        target = get_object_or_404(User, username=username)
        return _profile_response(target, request)


class ProfileFollowAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, username):
        target = get_object_or_404(User, username=username)
        Follow.objects.get_or_create(user=request.user, target=target)
        return _profile_response(target, request)

    def delete(self, request, username):
        target = get_object_or_404(User, username=username)
        Follow.objects.filter(user=request.user, target=target).delete()
        return _profile_response(target, request)
