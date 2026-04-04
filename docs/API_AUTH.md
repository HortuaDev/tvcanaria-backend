# API — Autenticación

**Base path:** `/api/auth`

Todos los endpoints de este módulo son **públicos** (no requieren token JWT).

---

## POST `/api/auth/register`

Registra un nuevo usuario con autenticación local.

### Request body

```json
{
  "username": "juanperez",
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "password": "Secreto123"
}
```

### Validaciones

| Campo       | Regla                                                                           |
| ----------- | ------------------------------------------------------------------------------- |
| `username`  | Obligatorio · 3–25 caracteres · solo letras, números y `_`                      |
| `firstName` | Obligatorio · máx. 50 caracteres                                                |
| `lastName`  | Obligatorio · máx. 50 caracteres                                                |
| `email`     | Obligatorio · formato email válido                                              |
| `password`  | Obligatorio · mínimo 8 caracteres · debe contener mayúscula, minúscula y número |

### Response `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "juanperez",
  "email": "juan@example.com",
  "role": "READER"
}
```

### Errores posibles

| Código | Causa                                                                   |
| ------ | ----------------------------------------------------------------------- |
| `400`  | Validación fallida (campos inválidos o faltantes)                       |
| `409`  | El email o username ya están registrados (`UserAlreadyExistsException`) |

---

## POST `/api/auth/login`

Autentica a un usuario con email o nombre de usuario y contraseña.

### Request body

```json
{
  "usernameOrEmail": "juanperez",
  "password": "Secreto123"
}
```

### Validaciones

| Campo             | Regla       |
| ----------------- | ----------- |
| `usernameOrEmail` | Obligatorio |
| `password`        | Obligatorio |

### Response `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "juanperez",
  "email": "juan@example.com",
  "role": "READER"
}
```

### Errores posibles

| Código | Causa                                                    |
| ------ | -------------------------------------------------------- |
| `400`  | Campos vacíos                                            |
| `401`  | Credenciales incorrectas (`InvalidCredentialsException`) |
| `403`  | Cuenta desactivada (`AccountDisabledException`)          |

---

## POST `/api/auth/google`

Autentica (o registra automáticamente) a un usuario mediante un token de identidad de Google.

### Request body

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```

### Validaciones

| Campo     | Regla       |
| --------- | ----------- |
| `idToken` | Obligatorio |

### Response `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 2,
  "username": "juan.perez",
  "email": "juan@gmail.com",
  "role": "READER"
}
```

### Comportamiento

1. Se valida el `idToken` contra los servidores de Google.
2. Si el usuario ya existe (por `providerId`), se autentica directamente.
3. Si no existe, se crea automáticamente con rol `READER` y `authProvider = "GOOGLE"`.
4. Se devuelve un token JWT propio del sistema.

### Errores posibles

| Código | Causa                                                        |
| ------ | ------------------------------------------------------------ |
| `400`  | `idToken` vacío o inválido                                   |
| `502`  | Error al comunicarse con Google (`ExternalServiceException`) |

---

## DTO de respuesta: `AuthResponse`

```java
String token       // Token JWT
String type        // Siempre "Bearer"
Integer userId     // ID del usuario en el sistema
String username    // Nombre de usuario
String email       // Correo electrónico
String role        // Rol: READER | REPORTER | MODERATOR | ADMIN
```

[⬅ Volver al README Principal](../README.md)
