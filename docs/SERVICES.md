# Servicios

**Paquete:** `com.tvcanaria.service`

Los servicios encapsulan toda la lógica de negocio de la aplicación. Los controladores delegan en ellos para realizar las operaciones, y a su vez los servicios usan los repositorios para acceder a los datos.

---

## AuthService

**Clase:** `com.tvcanaria.service.AuthService`

Gestiona el registro y la autenticación local de usuarios.

### `register(RegisterRequest)`

1. Verifica que el email no esté en uso → `UserAlreadyExistsException`
2. Verifica que el username no esté en uso → `UserAlreadyExistsException`
3. Crea el usuario con `role = READER`, `authProvider = "LOCAL"`, contraseña hasheada con BCrypt.
4. Persiste el usuario y genera un token JWT.
5. Retorna `AuthResponse` con el token y datos del usuario.

Operación marcada como `@Transactional`.

### `login(LoginRequest)`

1. Busca el usuario por email; si no, por username → `InvalidCredentialsException`
2. Verifica que la cuenta esté activa → `AccountDisabledException`
3. Verifica la contraseña con BCrypt → `InvalidCredentialsException`
4. Genera el token JWT y retorna `AuthResponse`.

---

## CategoryService

**Clase:** `com.tvcanaria.service.CategoryService`

Gestiona el ciclo de vida y la consulta de categorías.

### `getAllCategories()`

Devuelve todas las categorías como lista de `CategoryResponse`.

### `getCategoriesByIds(Set<Integer>)`

Resuelve IDs a entidades `Category`. Si algún ID no existe, lanza `ResourceNotFoundException` con los IDs faltantes.

### `getCategoryById(Integer)`

Obtiene una categoría por ID. Lanza `ResourceNotFoundException` si no existe.

### `updateCategory(Integer, Category)`

Actualiza el nombre de una categoría existente. Solo modifica el campo `name` si viene relleno.

### `deleteCategory(Integer)`

Elimina una categoría por ID. Lanza `ResourceNotFoundException` si no existe.

---

## CloudinaryService

**Clase:** `com.tvcanaria.service.CloudinaryService`

Gestiona las operaciones de almacenamiento de vídeo en Cloudinary.

### `uploadVideo(MultipartFile)`

Sube el archivo como recurso de tipo `video` a Cloudinary y devuelve el mapa de metadatos resultante (incluye `secure_url`, `public_id`, duración, etc.).

Lanza `ExternalServiceException` si falla la subida.

### `deleteVideoByUrl(String)` — `@Async`

Elimina un vídeo de Cloudinary a partir de su URL completa. La operación se ejecuta en **segundo plano** para no bloquear la respuesta al cliente. Los errores se registran en el log pero no se propagan.

**Lógica de extracción del `public_id`:**

La URL de Cloudinary tiene el formato:

```
https://res.cloudinary.com/{cloud}/video/upload/v{version}/{path}/{filename}.{ext}
```

El método extrae la parte `{path}/{filename}` eliminando el segmento de versión y la extensión.

---

## ReporterService

**Clase:** `com.tvcanaria.service.ReporterService`

Gestiona la consulta del perfil público de reporters.

### `getReporter(Integer reporterId)`

1. Busca el usuario por ID → `ResourceNotFoundException` si no existe.
2. Verifica que su rol sea `REPORTER` → `ForbiddenAccessException` si no lo es.
3. Retorna `ReporterProfileResponse` con los datos públicos.

---

## OAuth2Service

**Clase:** `com.tvcanaria.service.OAuth2Service`

Gestiona la autenticación y el registro de usuarios mediante Google OAuth2.

### `authenticateGoogleToken(String idToken)`

Valida el `idToken` de Google, obtiene los datos del usuario (email, nombre, `sub`) y:

- Si el usuario ya existe (por `providerId`), genera el JWT directamente.
- Si no existe, lo registra automáticamente con `role = READER` y `authProvider = "GOOGLE"`.
- Retorna `AuthResponse`.

### `processGoogleUser(OAuth2User)`

Variante usada por el flujo OAuth2 de Spring Security (navegador web). Recibe el objeto `OAuth2User` ya autenticado por Spring y aplica la misma lógica de obtener o crear al usuario.

[⬅ Volver al README Principal](../README.md)
