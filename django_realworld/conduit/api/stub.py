from rest_framework.response import Response
from rest_framework.views import APIView


class StubView(APIView):
    def _not_implemented(self, request, *args, **kwargs):
        return Response({"detail": "not implemented"}, status=501)

    get = post = put = delete = _not_implemented
