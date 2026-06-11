from rest_framework import serializers

from authentication.models import User
from authentication.services import generate_token

REQUIRED_MESSAGES = {"blank": "can't be empty", "required": "can't be empty", "null": "can't be empty"}
EMAIL_MESSAGES = dict(REQUIRED_MESSAGES, invalid="should be an email")


class UserSerializer(serializers.ModelSerializer):
    """Serializes the public user payload: {email, username, bio, image, token}."""

    token = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ["email", "username", "bio", "image", "token"]

    def get_token(self, obj):
        token = self.context.get("token")
        return token if token else generate_token(obj)


class RegisterSerializer(serializers.Serializer):
    email = serializers.EmailField(error_messages=EMAIL_MESSAGES)
    username = serializers.CharField(error_messages=REQUIRED_MESSAGES)
    password = serializers.CharField(error_messages=REQUIRED_MESSAGES)

    def validate_email(self, value):
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError("duplicated email")
        return value

    def validate_username(self, value):
        if User.objects.filter(username=value).exists():
            raise serializers.ValidationError("duplicated username")
        return value

    def create(self, validated_data):
        return User.objects.create_user(**validated_data)


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField(error_messages=EMAIL_MESSAGES)
    password = serializers.CharField(error_messages=REQUIRED_MESSAGES)

    def validate(self, data):
        try:
            user = User.objects.get(email=data["email"])
        except User.DoesNotExist:
            raise serializers.ValidationError("invalid email or password")
        if not user.check_password(data["password"]):
            raise serializers.ValidationError("invalid email or password")
        data["user"] = user
        return data


class UpdateUserSerializer(serializers.Serializer):
    email = serializers.EmailField(required=False, error_messages=EMAIL_MESSAGES)
    username = serializers.CharField(required=False)
    password = serializers.CharField(required=False)
    bio = serializers.CharField(required=False, allow_blank=True)
    image = serializers.CharField(required=False, allow_blank=True)

    def validate_email(self, value):
        if value and User.objects.filter(email=value).exclude(pk=self.instance.pk).exists():
            raise serializers.ValidationError("email already exist")
        return value

    def validate_username(self, value):
        if value and User.objects.filter(username=value).exclude(pk=self.instance.pk).exists():
            raise serializers.ValidationError("username already exist")
        return value

    def update(self, instance, validated_data):
        # Mirror User.update: only overwrite with non-empty provided values.
        for field in ["email", "username", "bio", "image"]:
            value = validated_data.get(field)
            if value:
                setattr(instance, field, value)
        password = validated_data.get("password")
        if password:
            instance.set_password(password)
        instance.save()
        return instance
