from rest_framework import status
from rest_framework.test import APITestCase

from authentication.models import User
from authentication.services import decode_token


class RegistrationTests(APITestCase):
    def test_register_success(self):
        payload = {"user": {"email": "john@jacob.com", "username": "johnjacob", "password": "123"}}
        response = self.client.post("/users", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        user = response.data["user"]
        self.assertEqual(user["email"], "john@jacob.com")
        self.assertEqual(user["username"], "johnjacob")
        self.assertEqual(user["bio"], "")
        self.assertEqual(user["image"], "https://static.productionready.io/images/smiley-cyrus.jpg")
        self.assertTrue(user["token"])
        created = User.objects.get(email="john@jacob.com")
        self.assertEqual(decode_token(user["token"]), str(created.id))
        self.assertTrue(created.check_password("123"))

    def test_register_blank_username(self):
        payload = {"user": {"email": "john@jacob.com", "username": "", "password": "123"}}
        response = self.client.post("/users", payload, format="json")
        self.assertEqual(response.status_code, 422)
        self.assertIn("errors", response.data)
        self.assertIn("body", response.data["errors"])

    def test_register_invalid_email(self):
        payload = {"user": {"email": "not-an-email", "username": "johnjacob", "password": "123"}}
        response = self.client.post("/users", payload, format="json")
        self.assertEqual(response.status_code, 422)
        self.assertIn("should be an email", response.data["errors"]["body"])

    def test_register_duplicated_username(self):
        User.objects.create_user(email="a@b.com", username="johnjacob", password="123")
        payload = {"user": {"email": "john@jacob.com", "username": "johnjacob", "password": "123"}}
        response = self.client.post("/users", payload, format="json")
        self.assertEqual(response.status_code, 422)
        self.assertIn("duplicated username", response.data["errors"]["body"])


class LoginTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            email="john@jacob.com", username="johnjacob", password="123"
        )

    def test_login_success(self):
        payload = {"user": {"email": "john@jacob.com", "password": "123"}}
        response = self.client.post("/users/login", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["user"]["email"], "john@jacob.com")
        self.assertEqual(decode_token(response.data["user"]["token"]), str(self.user.id))

    def test_login_wrong_password(self):
        payload = {"user": {"email": "john@jacob.com", "password": "wrong"}}
        response = self.client.post("/users/login", payload, format="json")
        self.assertEqual(response.status_code, 422)
        self.assertIn("invalid email or password", response.data["errors"]["body"])

    def test_login_unknown_email(self):
        payload = {"user": {"email": "nobody@nowhere.com", "password": "123"}}
        response = self.client.post("/users/login", payload, format="json")
        self.assertEqual(response.status_code, 422)
        self.assertIn("invalid email or password", response.data["errors"]["body"])


class CurrentUserTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            email="john@jacob.com", username="johnjacob", password="123"
        )
        login = self.client.post(
            "/users/login",
            {"user": {"email": "john@jacob.com", "password": "123"}},
            format="json",
        )
        self.token = login.data["user"]["token"]

    def _auth(self):
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {self.token}")

    def test_get_current_user_with_token(self):
        self._auth()
        response = self.client.get("/user")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["user"]["email"], "john@jacob.com")
        self.assertEqual(response.data["user"]["token"], self.token)

    def test_get_current_user_without_token(self):
        response = self.client.get("/user")
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_get_current_user_invalid_token(self):
        self.client.credentials(HTTP_AUTHORIZATION="Token garbage")
        response = self.client.get("/user")
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_update_current_user(self):
        self._auth()
        payload = {"user": {"email": "new@example.com", "bio": "updated"}}
        response = self.client.put("/user", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["user"]["email"], "new@example.com")
        self.assertEqual(response.data["user"]["bio"], "updated")
        self.user.refresh_from_db()
        self.assertEqual(self.user.email, "new@example.com")

    def test_update_current_user_email_taken(self):
        User.objects.create_user(email="taken@example.com", username="other", password="123")
        self._auth()
        payload = {"user": {"email": "taken@example.com"}}
        response = self.client.put("/user", payload, format="json")
        self.assertEqual(response.status_code, 422)
        self.assertIn("email already exist", response.data["errors"]["body"])
