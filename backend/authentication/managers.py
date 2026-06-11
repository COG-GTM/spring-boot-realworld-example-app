from django.conf import settings
from django.contrib.auth.base_user import BaseUserManager


class UserManager(BaseUserManager):
    """Mirrors UserService.createUser: default image when none supplied."""

    use_in_migrations = True

    def create_user(self, email, username, password=None, **extra_fields):
        if not email:
            raise ValueError("Users must have an email address")
        if not username:
            raise ValueError("Users must have a username")

        extra_fields.setdefault("bio", "")
        if not extra_fields.get("image"):
            extra_fields["image"] = settings.DEFAULT_USER_IMAGE

        user = self.model(email=self.normalize_email(email), username=username, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, username, password=None, **extra_fields):
        return self.create_user(email, username, password, **extra_fields)
