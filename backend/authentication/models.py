import uuid

from django.contrib.auth.base_user import AbstractBaseUser
from django.db import models

from authentication.managers import UserManager


class User(AbstractBaseUser):
    """Mirrors the ``users`` table (V1__create_tables.sql) and ``User.java``."""

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    email = models.EmailField(unique=True)
    username = models.CharField(max_length=255, unique=True)
    bio = models.TextField(blank=True, default="")
    image = models.CharField(max_length=511, blank=True, default="")

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = ["username"]

    objects = UserManager()

    class Meta:
        db_table = "users"

    def __str__(self):
        return self.username
