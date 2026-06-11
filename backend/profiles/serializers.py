from rest_framework import serializers

from profiles.models import Follow


class ProfileSerializer(serializers.Serializer):
    """Serializes a user as a public profile: {username, bio, image, following}.

    ``following`` reflects whether the request user follows this profile
    (False when anonymous). Reused by the articles app in a later part.
    """

    username = serializers.CharField()
    bio = serializers.CharField()
    image = serializers.CharField()
    following = serializers.SerializerMethodField()

    def get_following(self, obj):
        request = self.context.get("request")
        user = getattr(request, "user", None)
        if not user:
            return False
        return Follow.objects.filter(user=user, target=obj).exists()
