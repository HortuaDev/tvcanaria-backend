# API — Usuarios y Perfiles

Este documento cubre los DTOs y endpoints relacionados con perfiles de usuario, consulta de reporters y administración de usuarios.

---

## Perfil público de reporter

**Base path:** `/api/reporter`

### GET `/api/reporter/{id}`

Devuelve el perfil público (canal) de un usuario con rol `REPORTER`.

#### Path param

| Parámetro | Tipo      | Descripción     |
| --------- | --------- | --------------- |
| `id`      | `Integer` | ID del reporter |

#### Response `200 OK`

```json
{
  "username": "reportero1",
  "firstName": "María",
  "lastName": "González",
  "createdAt": "2024-01-10T09:00:00"
}
```

#### Errores posibles

| Código | Causa                                                                       |
| ------ | --------------------------------------------------------------------------- |
| `404`  | Usuario no encontrado (`ResourceNotFoundException`)                         |
| `403`  | El usuario existe pero no tiene rol `REPORTER` (`ForbiddenAccessException`) |

---

## DTOs de perfil

### `UserProfileResponse` — Perfil completo del usuario autenticado

```json
{
  "userId": 5,
  "username": "juanperez",
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "role": "Reportero",
  "isActive": true,
  "authProvider": "LOCAL",
  "createdAt": "2024-01-15T12:00:00"
}
```

> El campo `role` se traduce al español: `READER` → `Lector`, `REPORTER` → `Reportero`, `MODERATOR` → `Moderador`, `ADMIN` → `Administrador`.

### `UpdateProfileRequest` — Actualizar perfil propio

Todos los campos son opcionales.

```json
{
  "firstName": "Juan",
  "lastName": "Pérez Rodríguez",
  "email": "nuevo@example.com"
}
```

| Campo       | Reglas                          |
| ----------- | ------------------------------- |
| `firstName` | Opcional · 2–50 caracteres      |
| `lastName`  | Opcional · 2–50 caracteres      |
| `email`     | Opcional · formato email válido |

### `ReporterProfileResponse` — Perfil público del reporter

```json
{
  "username": "reportero1",
  "firstName": "María",
  "lastName": "González",
  "createdAt": "2024-01-10T09:00:00"
}
```

---

## Administración de usuarios (Admin)

Los siguientes DTOs son usados exclusivamente por usuarios con rol `ADMIN`.

### `CreateUserAdminRequest` — Crear usuario

```json
{
  "firstName": "Ana",
  "lastName": "López",
  "username": "analopez",
  "email": "ana@example.com",
  "password": "Segura123",
  "role": "REPORTER"
}
```

| Campo       | Reglas                              |
| ----------- | ----------------------------------- |
| `firstName` | Obligatorio                         |
| `lastName`  | Obligatorio                         |
| `username`  | Obligatorio                         |
| `email`     | Obligatorio · formato email válido  |
| `password`  | Obligatorio                         |
| `role`      | Obligatorio · valor del enum `Role` |

### `UpdateUserAdminRequest` — Actualizar usuario

```json
{
  "firstName": "Ana",
  "lastName": "López Martín",
  "username": "analopez2",
  "email": "ana2@example.com",
  "role": "MODERATOR"
}
```

| Campo  | Reglas                                           |
| ------ | ------------------------------------------------ |
| Todos  | Obligatorios (actualización completa del perfil) |
| `role` | Valor del enum `Role`                            |

### `UserSummaryResponse` — Listado de usuarios

```json
{
  "userId": 3,
  "username": "analopez",
  "firstName": "Ana",
  "lastName": "López",
  "email": "ana@example.com",
  "role": "REPORTER"
}
```

---

## Moderación

### `ModeratorRequest` — Asignar moderador

```json
{
  "moderatorId": 8
}
```

| Campo         | Reglas      |
| ------------- | ----------- |
| `moderatorId` | Obligatorio |

### `ModeratorResponse` — Detalle de relación moderador-reporter

```json
{
  "id": 12,
  "reporterId": 5,
  "reporterUsername": "reportero1",
  "reporterFirstName": "María",
  "reporterLastName": "González",
  "reporterEmail": "maria@example.com",
  "moderatorId": 8,
  "moderatorUsername": "moderador1",
  "moderatorFirstName": "Carlos",
  "moderatorLastName": "Ruiz",
  "moderatorEmail": "carlos@example.com",
  "status": "PENDING",
  "createdAt": "2024-03-01T08:30:00"
}
```

El campo `status` puede ser `PENDING`, `ACCEPTED` o `REJECTED`.

---

## Comentarios

### `CommentRequest` — Crear comentario

```json
{
  "comment": "Excelente reportaje",
  "rating": 4.5,
  "articleId": 42
}
```

| Campo       | Reglas                            |
| ----------- | --------------------------------- |
| `comment`   | Obligatorio                       |
| `rating`    | Obligatorio · entre `0.5` y `5.0` |
| `articleId` | Obligatorio                       |

### `CommentResponse` — Respuesta de comentario

```json
{
  "commentId": 101,
  "comment": "Excelente reportaje",
  "createdAt": "2024-03-15T11:00:00",
  "offenseCount": 0,
  "canReport": true,
  "rating": 4.5,
  "username": "juanperez",
  "authorId": 5,
  "articleId": 42
}
```

El campo `canReport` indica si el usuario actual puede reportar ese comentario (no ha reportado ya).

---

## Respuesta de error estándar: `ErrorResponse`

```json
{
  "timestamp": "2024-03-15T10:45:00",
  "status": 404,
  "error": "Not Found",
  "message": "Categoría no encontrada con ID: 99",
  "path": "/api/categories/99"
}
```

[⬅ Volver al README Principal](../README.md)
