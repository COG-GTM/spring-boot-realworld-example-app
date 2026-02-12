## Authentication

This API uses JWT tokens.

### REST

- Include header: `Authorization: Token <jwt>` for authenticated endpoints.
- Obtain token via:
  - POST `/users` (register) → `{ user: { token, ... } }`
  - POST `/users/login` (login) → `{ user: { token, ... } }`

Example:
```bash
curl -H 'Content-Type: application/json' \
     -d '{"user": {"email":"jake@realworld.io","password":"jakejake"}}' \
     http://localhost:8080/users/login
```

Use returned token:
```bash
curl -H 'Authorization: Token <jwt>' http://localhost:8080/user
```

### GraphQL

- Include header: `Authorization: Bearer <jwt>`.

