# Excepciones personalizadas

**Paquete:** `com.tvcanaria.exception`

Todas las excepciones del sistema extienden `RuntimeException` y se usan para representar situaciones de error concretas del dominio. Un `GlobalExceptionHandler` (handler global) las intercepta y las transforma en respuestas HTTP estandarizadas con el DTO `ErrorResponse`.

---

## Catálogo de excepciones

| Excepción                     | HTTP               | Cuándo se lanza                                                       |
| ----------------------------- | ------------------ | --------------------------------------------------------------------- |
| `AccountDisabledException`    | `403 Forbidden`    | La cuenta del usuario está desactivada (`isActive = false`)           |
| `BadRequestException`         | `400 Bad Request`  | Petición semánticamente incorrecta no cubierta por validación de bean |
| `DuplicateResourceException`  | `409 Conflict`     | Se intenta crear un recurso que ya existe (distinto de usuario)       |
| `ExternalServiceException`    | `502 Bad Gateway`  | Error al comunicarse con un servicio externo (Cloudinary, Google)     |
| `ForbiddenAccessException`    | `403 Forbidden`    | El usuario no tiene permiso para realizar la operación                |
| `InvalidCredentialsException` | `401 Unauthorized` | Email/usuario o contraseña incorrectos en el login                    |
| `ResourceNotFoundException`   | `404 Not Found`    | El recurso solicitado no existe en la base de datos                   |
| `UserAlreadyExistsException`  | `409 Conflict`     | Email o username ya registrado al intentar crear un usuario           |

---

## Formato de respuesta de error

Todas las excepciones se serializan usando el DTO `ErrorResponse`:

```json
{
  "timestamp": "2024-03-15T10:45:00",
  "status": 404,
  "error": "Not Found",
  "message": "Categoría no encontrada con ID: 99",
  "path": "/api/categories/99"
}
```

| Campo       | Descripción                                          |
| ----------- | ---------------------------------------------------- |
| `timestamp` | Momento exacto del error (UTC local del servidor)    |
| `status`    | Código HTTP numérico                                 |
| `error`     | Descripción estándar HTTP del código                 |
| `message`   | Mensaje descriptivo del error (útil para el cliente) |
| `path`      | Ruta del endpoint que generó el error                |

---

## Detalle de cada excepción

### `AccountDisabledException`

Se lanza en el servicio de login cuando el usuario existe pero `isActive` es `false`. Indica que la cuenta ha sido desactivada por un administrador.

```java
throw new AccountDisabledException("Tu cuenta ha sido desactivada. Contacta al administrador");
```

---

### `BadRequestException`

Para casos de datos inválidos que no son capturables con anotaciones de validación estándar (`@NotBlank`, etc.), como combinaciones de campos incorrectas o datos en formato inesperado.

---

### `DuplicateResourceException`

Para recursos duplicados fuera del ámbito de usuarios (por ejemplo, intentar crear una categoría con un nombre ya existente).

---

### `ExternalServiceException`

Se lanza cuando falla una llamada a un servicio de terceros. Actualmente utilizada en `CloudinaryService` al fallar la subida de un vídeo.

```java
throw new ExternalServiceException("Error al subir el archivo a Cloudinary: Inténtalo de nuevo más tarde.");
```

---

### `ForbiddenAccessException`

Se lanza cuando el usuario autenticado no cumple los requisitos de rol o propiedad para realizar la operación. Por ejemplo, intentar ver el perfil público de un usuario que no es reporter.

```java
throw new ForbiddenAccessException("El usuario no es reportero, no tiene un perfil público.");
```

---

### `InvalidCredentialsException`

Se lanza en el login cuando las credenciales no coinciden. El mensaje es intencionalmente genérico para no revelar si el usuario existe.

```java
throw new InvalidCredentialsException("Email/usuario o contraseña incorrectos");
```

---

### `ResourceNotFoundException`

La excepción más utilizada. Se lanza en cualquier operación de consulta cuando el recurso no existe en la base de datos.

```java
throw new ResourceNotFoundException("Categoría no encontrada con ID: " + categoryId);
throw new ResourceNotFoundException("Categorías no encontradas con IDs: " + notFoundIds);
throw new ResourceNotFoundException("Usuario no encontrado con ID: " + reporterId);
```

---

### `UserAlreadyExistsException`

Se lanza durante el registro cuando el email o el username ya están en uso.

```java
throw new UserAlreadyExistsException("El email ya está registrado");
throw new UserAlreadyExistsException("El nombre de usuario ya está en uso");
```

[⬅ Volver al README Principal](../README.md)
