from rest_framework import status
from rest_framework.test import APITestCase

from authentication.models import User
from profiles.models import Follow


class ProfileTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            email="john@jacob.com", username="johnjacob", password="123"
        )
        self.other = User.objects.create_user(
            email="username@test.com", username="username", password="123"
        )
        login = self.client.post(
            "/users/login",
            {"user": {"email": "john@jacob.com", "password": "123"}},
            format="json",
        )
        self.token = login.data["user"]["token"]

    def _auth(self):
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {self.token}")

    def test_get_profile_anonymous(self):
        response = self.client.get(f"/profiles/{self.other.username}")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        profile = response.data["profile"]
        self.assertEqual(profile["username"], "username")
        self.assertFalse(profile["following"])

    def test_get_profile_not_found(self):
        response = self.client.get("/profiles/nope")
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_follow_user(self):
        self._auth()
        response = self.client.post(f"/profiles/{self.other.username}/follow")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data["profile"]["following"])
        self.assertTrue(Follow.objects.filter(user=self.user, target=self.other).exists())

    def test_follow_requires_auth(self):
        response = self.client.post(f"/profiles/{self.other.username}/follow")
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_unfollow_user(self):
        Follow.objects.create(user=self.user, target=self.other)
        self._auth()
        response = self.client.delete(f"/profiles/{self.other.username}/follow")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["profile"]["following"])
        self.assertFalse(Follow.objects.filter(user=self.user, target=self.other).exists())
