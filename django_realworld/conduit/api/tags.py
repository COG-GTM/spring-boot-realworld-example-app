"""Port of ``TagsApi``."""

from rest_framework.response import Response
from rest_framework.views import APIView

from conduit.application import queries


class TagsView(APIView):
    def get(self, request):
        return Response({"tags": queries.all_tags()})
