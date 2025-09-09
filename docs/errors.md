## Errors

Validation and authentication errors return JSON with an `errors` object.

### 422 Unprocessable Entity (validation)

Structure:
```json
{
  "errors": {
    "fieldName": ["message1", "message2"]
  }
}
```

Example (register with duplicate email):
```json
{
  "errors": {
    "email": ["should be an email", "already exists"]
  }
}
```

### 422 Unprocessable Entity (login failure)

Structure:
```json
{ "message": "email or password is invalid" }
```

### 401/403

- Endpoints requiring auth may return 401/403 depending on configuration.

