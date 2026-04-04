# Seguridad

---

## Visión general

El sistema utiliza **JWT (JSON Web Tokens)** con firma HMAC-SHA256 como mecanismo de autenticación principal. Todos los endpoints, salvo los de autenticación, requieren un token válido en la cabecera `Authorization`.

Adicionalmente se soporta **Google OAuth2** para el flujo de autenticación mediante navegador web.

---

## JwtTokenProvider

**Clase:** `com.tvcanaria.security.JwtTokenProvider`

Componente central que gestiona la generación y validación de tokens JWT.

### Generación de token

```java
String generateToken(User user)
```

Genera un JWT firmado con HS256 que incluye los siguientes claims:

| Claim      | Valor                           |
| ---------- | ------------------------------- |
| `sub`      | ID del usuario (como String)    |
| `email`    | Email del usuario               |
| `username` | Nombre de usuario               |
| `role`     | Nombre del rol (ej. `REPORTER`) |
| `iat`      | Timestamp de emisión            |
| `exp`      | Timestamp de expiración         |

La clave de firma se deriva de la propiedad `jwt.secret` y la expiración de `jwt.expiration` (en ms).

### Validación

```java
boolean validateToken(String token)
```

Devuelve `true` si el token es válido (firma correcta, no expirado). Captura internamente `JwtException` e `IllegalArgumentException`.

### Extracción de datos

```java
Integer getUserIdFromToken(String token)  // Extrae el userId del claim 'sub'
String getRoleFromToken(String token)     // Extrae el rol del claim 'role'
```

---

## JwtAuthenticationFilter

**Clase:** `com.tvcanaria.security.JwtAuthenticationFilter`

Filtro de Spring Security que se ejecuta **una sola vez por petición** (`OncePerRequestFilter`).

### Flujo de procesamiento

```
Petición HTTP
     │
     ▼
¿Es /api/auth/register, /login o /google?
     │ Sí → deja pasar sin validar
     │ No ↓
     ▼
Extrae token del header "Authorization: Bearer <token>"
     │
     ▼
¿Token presente y válido?
     │ Sí → extrae userId y role
     │      → crea UsernamePasswordAuthenticationToken
     │      → lo registra en SecurityContextHolder
     │ No → continúa sin autenticar (Spring Security lo rechazará si el endpoint lo requiere)
     ▼
Continúa la cadena de filtros
```

### Endpoints excluidos de validación JWT

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/google`

---

## OAuth2AuthenticationSuccessHandler

**Clase:** `com.tvcanaria.security.OAuth2AuthenticationSuccessHandler`

Maneja el callback de éxito del flujo OAuth2 de Google (flujo navegador web, distinto del endpoint `/api/auth/google`).

### Flujo

1. Spring Security completa el handshake OAuth2 con Google.
2. Este handler recibe el `OAuth2User` autenticado.
3. Delega en `OAuth2Service.processGoogleUser()` para obtener o crear al usuario.
4. Redirige al frontend (`http://localhost:3000/auth/callback`) con los datos del usuario como query params:

```
http://localhost:3000/auth/callback?token=...&userId=...&username=...&email=...&role=...
```

> En producción, actualizar la URL de redirección al dominio real del frontend.

---

## Autorización por roles

La autorización se gestiona a nivel de servicio o mediante anotaciones de Spring Security. El rol del usuario autenticado está disponible como `GrantedAuthority` en el `SecurityContext`:

```java
// Recuperar el userId del usuario autenticado en un controlador:
Integer userId = (Integer) SecurityContextHolder.getContext()
    .getAuthentication().getPrincipal();
```

### Restricciones por rol (ejemplos)

| Operación                                    | Rol mínimo requerido |
| -------------------------------------------- | -------------------- |
| Ver artículos públicos                       | Ninguno (público)    |
| Comentar y valorar                           | `READER`             |
| Publicar artículos                           | `REPORTER`           |
| Moderar comentarios                          | `MODERATOR`          |
| Administrar usuarios                         | `ADMIN`              |
| Ver todos los artículos (incluyendo ocultos) | `ADMIN`              |

[⬅ Volver al README Principal](../README.md)
