from django.conf import settings
from django.db import models


class Follow(models.Model):
    """Directed follow relation, mirroring the ``follows`` table and FollowRelation.java.

    ``user`` follows ``target`` (stored in the ``follow_id`` column for parity).
    """

    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="following_set",
        db_column="user_id",
    )
    target = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="followers_set",
        db_column="follow_id",
    )

    class Meta:
        db_table = "follows"
        unique_together = ("user", "target")
