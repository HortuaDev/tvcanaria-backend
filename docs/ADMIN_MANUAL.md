# Manual de Administración del Sistema — TV Canaria (Backend)

Guía funcional para administradores técnicos del backend de TV Canaria. Describe cómo operar, configurar y mantener el sistema.

---

## Índice

1. [Visión general del sistema](#1-visión-general-del-sistema)
2. [Requisitos y configuración del entorno](#2-requisitos-y-configuración-del-entorno)
3. [Autenticación y seguridad](#3-autenticación-y-seguridad)
4. [Gestión de usuarios](#4-gestión-de-usuarios)
5. [Gestión de artículos](#5-gestión-de-artículos)
6. [Gestión de categorías](#6-gestión-de-categorías)
7. [Gestión de comentarios](#7-gestión-de-comentarios)
8. [Sistema de moderación](#8-sistema-de-moderación)
9. [Almacenamiento de vídeo (Cloudinary)](#9-almacenamiento-de-vídeo-cloudinary)
10. [Referencia de la API REST](#10-referencia-de-la-api-rest)
11. [Gestión de errores](#11-gestión-de-errores)
12. [Mantenimiento y operaciones](#12-mantenimiento-y-operaciones)

---

## 1. Visión general del sistema

El backend de TV Canaria es una API REST construida con Spring Boot 3 que gestiona toda la lógica de negocio de la plataforma: autenticación de usuarios, publicación de artículos, sistema de comentarios y valoraciones, moderación de contenido y administración de cuentas.

### 1.1 Arquitectura en capas

El sistema sigue una arquitectura en capas estándar de Spring Boot:

| Capa           | Responsabilidad                                                         |
| -------------- | ----------------------------------------------------------------------- |
| **Controller** | Expone los endpoints REST. Valida la entrada y delega en los servicios. |
| **Service**    | Contiene toda la lógica de negocio.                                     |
| **Repository** | Accede a la base de datos mediante Spring Data JPA.                     |
| **Entity**     | Mapea las tablas de la base de datos.                                   |

### 1.2 Stack tecnológico

| Componente              | Tecnología                            |
| ----------------------- | ------------------------------------- |
| Lenguaje                | Java 17                               |
| Framework               | Spring Boot 3                         |
| Seguridad               | Spring Security + JWT (jjwt, HS256)   |
| ORM / Base de datos     | Spring Data JPA + Hibernate + MySQL 8 |
| Almacenamiento de vídeo | Cloudinary                            |
| Autenticación externa   | Google OAuth2 (ID Token)              |
| Build                   | Maven                                 |
| Utilidades              | Lombok                                |

### 1.3 Roles del sistema

| Rol         | Descripción funcional                                                         |
| ----------- | ----------------------------------------------------------------------------- |
| `READER`    | Usuario base. Solo puede leer artículos, comentar y valorar.                  |
| `REPORTER`  | Publica artículos con vídeo. Gestiona sus publicaciones y asigna moderadores. |
| `MODERATOR` | Modera comentarios de los reporters que le han asignado.                      |
| `ADMIN`     | Acceso completo. Gestiona usuarios, artículos y comentarios sin restricción.  |

---

## 2. Requisitos y configuración del entorno

### 2.1 Requisitos previos

- Java 17 o superior
- Maven 3.8 o superior
- MySQL 8 con una base de datos creada para la aplicación
- Cuenta activa en Cloudinary
- Credenciales de Google OAuth2 (Client ID y Secret)

### 2.2 Variables de configuración

Todas las variables sensibles deben configurarse como variables de entorno o en el fichero `application.properties`. **Nunca incluyas credenciales en el repositorio.**

**Base de datos:**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tvcanaria
spring.datasource.username=YOUR_USER
spring.datasource.password=YOUR_PASS
```

**JWT:**

```properties
jwt.secret=CLAVE_SECRETA_MINIMO_32_CARACTERES
jwt.expiration=86400000
```

**Cloudinary:**

```properties
cloudinary.cloud-name=YOUR_CLOUD_NAME
cloudinary.api-key=YOUR_API_KEY
cloudinary.api-secret=YOUR_API_SECRET
```

**Google OAuth2:**

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_SECRET
```

> **⛔ Importante:** Nunca incluyas credenciales reales en el repositorio de código. Usa variables de entorno o un gestor de secretos en producción.

### 2.3 Arrancar la aplicación

```bash
mvn spring-boot:run
```

La API arranca por defecto en el puerto 8080. Para cambiarlo:

```properties
server.port=8080
```

### 2.4 Orígenes CORS permitidos

Los orígenes permitidos están configurados en `CorsConfig`. Por defecto permite localhost en varios puertos para desarrollo. **En producción, actualiza la lista con los dominios reales del frontend.**

| Origen                   | Uso                |
| ------------------------ | ------------------ |
| `http://localhost:19006` | Expo web           |
| `http://localhost:8081`  | Metro bundler      |
| `http://localhost:3000`  | Puerto alternativo |
| `exp://localhost:8081`   | Expo Go            |
| `http://10.0.2.2:8082`   | Emulador Android   |

---

## 3. Autenticación y seguridad

### 3.1 Mecanismo de autenticación

El sistema usa **JWT (JSON Web Token)** firmados con HMAC-SHA256. El token se genera tras un login exitoso y debe incluirse en todas las peticiones protegidas mediante la cabecera:

```
Authorization: Bearer <token_jwt>
```

### 3.2 Endpoints públicos (sin token requerido)

| Endpoint                  | Descripción                          |
| ------------------------- | ------------------------------------ |
| `POST /api/auth/register` | Registro de nueva cuenta             |
| `POST /api/auth/login`    | Login con email/usuario y contraseña |
| `POST /api/auth/google`   | Login/registro con token de Google   |

Todos los demás endpoints requieren token JWT válido.

### 3.3 Contenido del token JWT

| Claim      | Valor                                                       |
| ---------- | ----------------------------------------------------------- |
| `sub`      | ID del usuario (`userId`)                                   |
| `email`    | Correo electrónico                                          |
| `username` | Nombre de usuario                                           |
| `role`     | Rol del usuario: `READER`, `REPORTER`, `MODERATOR`, `ADMIN` |
| `iat`      | Timestamp de emisión                                        |
| `exp`      | Timestamp de expiración                                     |

### 3.4 Expiración del token

El tiempo de expiración se configura en milisegundos con `jwt.expiration`. El valor por defecto es `86400000` ms (24 horas). Tras la expiración el usuario debe volver a autenticarse.

### 3.5 Login con Google (endpoint móvil/web)

El endpoint `POST /api/auth/google` recibe un `idToken` emitido por Google. El backend lo valida con los servidores de Google, extrae el email y el `providerId`, y si el usuario no existe lo crea automáticamente con rol `READER` y `authProvider='GOOGLE'`. Devuelve un JWT propio del sistema.

### 3.6 Flujo OAuth2 por navegador

Para el flujo completo de OAuth2 por navegador, Spring Security gestiona el handshake. Tras el login exitoso, `OAuth2AuthenticationSuccessHandler` redirige al frontend con el token como parámetro de query:

```
http://localhost:3000/auth/callback?token=...&userId=...&username=...&email=...&role=...
```

> **Nota:** En producción actualiza esta URL de redirección al dominio real del frontend en `OAuth2AuthenticationSuccessHandler`.

---

## 4. Gestión de usuarios

### 4.1 Estructura de la entidad User

| Campo                    | Descripción                                        |
| ------------------------ | -------------------------------------------------- |
| `userId`                 | Identificador único (PK, auto-incremental)         |
| `username`               | Nombre de usuario único (máx. 25 caracteres)       |
| `firstName` / `lastName` | Nombre y apellidos                                 |
| `email`                  | Correo electrónico único                           |
| `passwordHash`           | Contraseña hasheada con BCrypt                     |
| `role`                   | Enum: `READER`, `REPORTER`, `MODERATOR`, `ADMIN`   |
| `isActive`               | `true` = cuenta activa. `false` = cuenta bloqueada |
| `authProvider`           | `LOCAL` o `GOOGLE`                                 |
| `providerId`             | ID externo de Google (solo cuentas OAuth2)         |
| `createdAt`              | Fecha de registro (se genera automáticamente)      |

### 4.2 Validaciones de registro

- **Username:** 3–25 caracteres, solo letras, números y guión bajo.
- **Email:** formato válido de correo electrónico.
- **Password:** mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número.

### 4.3 Operaciones de administración (solo ADMIN)

| Método  | Endpoint                 | Descripción                                    |
| ------- | ------------------------ | ---------------------------------------------- |
| `GET`   | `/api/users/search`      | Búsqueda paginada con filtros de texto y fecha |
| `POST`  | `/api/users`             | Crear usuario con rol específico               |
| `PUT`   | `/api/users/{id}`        | Actualizar datos del usuario                   |
| `PATCH` | `/api/users/{id}/status` | Activar o desactivar cuenta                    |

### 4.4 Búsqueda de usuarios

El endpoint `GET /api/users/search` acepta los parámetros:

- `query` — texto libre que busca sobre `username`, `firstName` y `lastName` (case-insensitive).
- `dateFrom` / `dateTo` — rango de fechas de registro.
- `page` / `size` — paginación.

### 4.5 Desactivar / activar una cuenta

El endpoint `PATCH /api/users/{id}/status` alterna el estado `isActive` del usuario. Un usuario con `isActive = false` recibe un error `403 Forbidden` al intentar iniciar sesión.

### 4.6 Endpoints para el perfil propio del usuario autenticado

| Método  | Endpoint                     | Descripción                            |
| ------- | ---------------------------- | -------------------------------------- |
| `GET`   | `/api/users/profile`         | Obtener perfil del usuario autenticado |
| `PUT`   | `/api/users/profile`         | Actualizar nombre, apellidos o email   |
| `GET`   | `/api/users/{id}/categories` | Categorías preferidas del usuario      |
| `PUT`   | `/api/users/{id}/categories` | Actualizar categorías preferidas       |
| `PATCH` | `/api/users/{id}/status`     | Darse de baja (propio usuario)         |

---

## 5. Gestión de artículos

### 5.1 Estructura del artículo

| Campo         | Descripción                                                     |
| ------------- | --------------------------------------------------------------- |
| `articleId`   | Identificador único (PK)                                        |
| `title`       | Título (máx. 150 caracteres, obligatorio)                       |
| `description` | Descripción larga (TEXT, opcional)                              |
| `videoUrl`    | URL del vídeo almacenado en Cloudinary                          |
| `isHidden`    | Si el artículo está oculto para los usuarios (defecto: `false`) |
| `location`    | Localización geográfica (máx. 100 caracteres)                   |
| `rating`      | Valoración media calculada (BigDecimal 3,1)                     |
| `createdAt`   | Fecha de publicación (se genera automáticamente)                |
| `author`      | Usuario que publicó el artículo                                 |
| `categories`  | Conjunto de categorías asignadas                                |

### 5.2 Endpoints de artículos

| Método   | Endpoint                        | Descripción                             | Rol mínimo          |
| -------- | ------------------------------- | --------------------------------------- | ------------------- |
| `GET`    | `/api/articles`                 | Feed paginado de artículos visibles     | Autenticado         |
| `GET`    | `/api/articles/{id}`            | Artículo por ID                         | Autenticado         |
| `GET`    | `/api/articles/recommended`     | Artículos recomendados al usuario       | Autenticado         |
| `GET`    | `/api/articles/{id}/related`    | Artículos relacionados                  | Autenticado         |
| `GET`    | `/api/articles/search?keyword=` | Búsqueda por título                     | Autenticado         |
| `POST`   | `/api/articles/upload`          | Publicar artículo con vídeo (multipart) | `REPORTER`, `ADMIN` |
| `GET`    | `/api/articles/my-articles`     | Artículos del autor con filtros         | `REPORTER`, `ADMIN` |
| `PATCH`  | `/api/articles/{id}`            | Actualizar campos del artículo          | `REPORTER`, `ADMIN` |
| `DELETE` | `/api/articles/{id}`            | Eliminar artículo                       | `REPORTER`, `ADMIN` |
| `GET`    | `/api/articles/reporter/{id}`   | Artículos de un reporter concreto       | Autenticado         |

### 5.3 Cálculo de la valoración media

La valoración media de un artículo (campo `rating`) se calcula con una consulta SQL nativa que:

- Considera solo **el comentario más reciente de cada usuario** sobre ese artículo.
- Excluye comentarios con valoración inferior a `0.5`.
- Redondea el promedio al `0.5` más cercano usando `ROUND(AVG * 2) / 2`.
- Devuelve `0` si no hay valoraciones válidas.

La valoración se recalcula en cada nueva interacción de comentario. El campo `rating` de la tabla `article` se actualiza en cada operación.

### 5.4 Publicación de artículo con vídeo

El endpoint `POST /api/articles/upload` recibe una petición `multipart/form-data`. El servicio sube el vídeo a Cloudinary y guarda la URL devuelta en el campo `videoUrl` del artículo.

> **⛔ Importante:** Si la subida a Cloudinary falla, el artículo no se persiste y se devuelve al cliente un error `502 Bad Gateway`.

### 5.5 Eliminar un artículo

Al eliminar un artículo se eliminan en cascada todos sus comentarios y reportes asociados. El vídeo de Cloudinary se elimina de forma **asíncrona** (no bloquea la respuesta al cliente).

---

## 6. Gestión de categorías

Las categorías son etiquetas temáticas que se asignan a artículos y a las preferencias de los usuarios. Se gestionan directamente en la base de datos; actualmente no hay endpoint de creación en la API pública.

### 6.1 Endpoints de categorías

| Método | Endpoint               | Descripción                 | Rol mínimo  |
| ------ | ---------------------- | --------------------------- | ----------- |
| `GET`  | `/api/categories`      | Listar todas las categorías | Autenticado |
| `GET`  | `/api/categories/{id}` | Categoría por ID            | Autenticado |

### 6.2 Estructura

| Campo        | Descripción                                       |
| ------------ | ------------------------------------------------- |
| `categoryId` | Identificador único (PK)                          |
| `name`       | Nombre único de la categoría (máx. 50 caracteres) |

> **Nota:** Para crear, modificar o eliminar categorías es necesario acceder directamente a la base de datos o implementar un endpoint de administración. No existe actualmente en la API pública.

---

## 7. Gestión de comentarios

### 7.1 Estructura del comentario

| Campo          | Descripción                                   |
| -------------- | --------------------------------------------- |
| `commentId`    | Identificador único (PK)                      |
| `comment`      | Texto del comentario (TEXT)                   |
| `rating`       | Valoración de 0.5 a 5.0 (BigDecimal 2,1)      |
| `offenseCount` | Número de reportes recibidos (defecto: 0)     |
| `createdAt`    | Fecha de creación (se genera automáticamente) |
| `article`      | Artículo al que pertenece                     |
| `user`         | Usuario que lo publicó                        |

### 7.2 Endpoints de comentarios

| Método   | Endpoint                     | Descripción                          | Rol mínimo                       |
| -------- | ---------------------------- | ------------------------------------ | -------------------------------- |
| `GET`    | `/api/comments/article/{id}` | Comentarios paginados de un artículo | Autenticado                      |
| `POST`   | `/api/comments`              | Crear comentario con valoración      | Autenticado                      |
| `DELETE` | `/api/comments/{id}`         | Eliminar comentario                  | Autenticado (con permisos)       |
| `POST`   | `/api/comments/{id}/report`  | Reportar comentario                  | Autenticado                      |
| `GET`    | `/api/comments/reported`     | Comentarios reportados con filtros   | `MODERATOR`, `REPORTER`, `ADMIN` |
| `POST`   | `/api/comments/{id}/approve` | Aprobar comentario (mantenerlo)      | `MODERATOR`, `ADMIN`             |
| `DELETE` | `/api/comments/{id}/reject`  | Rechazar y eliminar comentario       | `MODERATOR`, `ADMIN`             |

### 7.3 Sistema de reportes

Cada reporte se almacena en la tabla `comment_report` con una restricción única `(comment_id, user_id)`, garantizando que cada usuario solo puede reportar el mismo comentario una vez. Cuando un reporte se procesa, se marca con `reviewed = true`.

### 7.4 Visibilidad de comentarios reportados por rol

| Rol         | Comentarios visibles                                                |
| ----------- | ------------------------------------------------------------------- |
| `ADMIN`     | Todos los comentarios con `offenseCount >= 1`.                      |
| `MODERATOR` | Solo en artículos de sus reporters asignados (status = `ACCEPTED`). |
| `REPORTER`  | Solo en sus propios artículos.                                      |

### 7.5 Permisos de borrado de comentarios

| Quién                      | Puede eliminar                                                     |
| -------------------------- | ------------------------------------------------------------------ |
| Propietario del comentario | Siempre.                                                           |
| `ADMIN`                    | Cualquier comentario.                                              |
| `MODERATOR`                | Comentarios con `offenseCount >= 5` en artículos de sus reporters. |

---

## 8. Sistema de moderación

El sistema permite a los reporters asignar moderadores para que supervisen los comentarios de su canal. La relación se gestiona mediante la entidad `ModeratorReporter`.

### 8.1 Estados de una solicitud

| Estado     | Descripción                                                                     |
| ---------- | ------------------------------------------------------------------------------- |
| `PENDING`  | Solicitud enviada. El moderador aún no ha respondido.                           |
| `ACCEPTED` | El moderador ha aceptado. Tiene visibilidad sobre los comentarios del reporter. |
| `REJECTED` | El moderador ha rechazado la solicitud.                                         |

### 8.2 Endpoints de moderación

| Método   | Endpoint                                    | Descripción                                         | Rol mínimo            |
| -------- | ------------------------------------------- | --------------------------------------------------- | --------------------- |
| `GET`    | `/api/moderators/my-requests`               | Solicitudes enviadas por el reporter                | `REPORTER`            |
| `GET`    | `/api/moderators/pending`                   | Solicitudes pendientes del moderador                | `MODERATOR`, `READER` |
| `POST`   | `/api/moderators/request`                   | Enviar solicitud a un moderador                     | `REPORTER`            |
| `DELETE` | `/api/moderators/{id}`                      | Cancelar solicitud                                  | `REPORTER`            |
| `PUT`    | `/api/moderators/{id}/accept`               | Aceptar solicitud                                   | `MODERATOR`           |
| `PUT`    | `/api/moderators/{id}/reject`               | Rechazar solicitud                                  | `MODERATOR`           |
| `GET`    | `/api/moderators/is-moderator/{reporterId}` | Verificar si el usuario es moderador de un reporter | Autenticado           |

### 8.3 Perfil público del reporter

```
GET /api/reporter/{id}
```

Devuelve `username`, `firstName`, `lastName` y `createdAt`. Si el usuario no tiene rol `REPORTER` se devuelve `403 Forbidden`.

---

## 9. Almacenamiento de vídeo (Cloudinary)

### 9.1 Subida de vídeo

Al publicar un artículo, `CloudinaryService.uploadVideo()` sube el fichero a Cloudinary con `resource_type='video'`. La respuesta contiene:

- `secure_url` — URL pública del vídeo, que se guarda en el artículo.
- `public_id` — Identificador del recurso en Cloudinary, usado para eliminación.

### 9.2 Eliminación asíncrona

Cuando se elimina un artículo, `CloudinaryService.deleteVideoByUrl()` extrae el `public_id` de la URL y elimina el recurso de Cloudinary. Esta operación se ejecuta en un hilo separado (`@Async`) para no bloquear la respuesta al cliente.

El método extrae el `public_id` eliminando el segmento de versión (`v12345/`) y la extensión del fichero de la URL completa.

> **Nota:** Los errores durante la eliminación asíncrona se registran en el log pero no se propagan al cliente. Revisa los logs periódicamente para detectar recursos huérfanos en Cloudinary.

### 9.3 Configuración requerida

```properties
cloudinary.cloud-name=YOUR_CLOUD_NAME
cloudinary.api-key=YOUR_API_KEY
cloudinary.api-secret=YOUR_API_SECRET
```

---

## 10. Referencia de la API REST

Resumen completo de todos los endpoints. La URL base es `/api`.

### 10.1 Autenticación (`/api/auth`)

| Método | Endpoint         | Descripción               | Rol mínimo |
| ------ | ---------------- | ------------------------- | ---------- |
| `POST` | `/auth/register` | Registro de nuevo usuario | Público    |
| `POST` | `/auth/login`    | Login con credenciales    | Público    |
| `POST` | `/auth/google`   | Login con token de Google | Público    |

### 10.2 Usuarios (`/api/users`)

| Método  | Endpoint                 | Descripción                      | Rol mínimo       |
| ------- | ------------------------ | -------------------------------- | ---------------- |
| `GET`   | `/users/profile`         | Perfil del usuario autenticado   | Autenticado      |
| `PUT`   | `/users/profile`         | Actualizar perfil propio         | Autenticado      |
| `GET`   | `/users/{id}/categories` | Categorías preferidas            | Autenticado      |
| `PUT`   | `/users/{id}/categories` | Actualizar categorías preferidas | Autenticado      |
| `GET`   | `/users/search`          | Buscar usuarios con filtros      | `ADMIN`          |
| `POST`  | `/users`                 | Crear usuario                    | `ADMIN`          |
| `PUT`   | `/users/{id}`            | Editar usuario                   | `ADMIN`          |
| `PATCH` | `/users/{id}/status`     | Activar / desactivar cuenta      | `ADMIN` / Propio |

### 10.3 Artículos (`/api/articles`)

| Método   | Endpoint                    | Descripción                         | Rol mínimo          |
| -------- | --------------------------- | ----------------------------------- | ------------------- |
| `GET`    | `/articles`                 | Feed paginado de artículos visibles | Autenticado         |
| `GET`    | `/articles/{id}`            | Artículo por ID                     | Autenticado         |
| `GET`    | `/articles/recommended`     | Recomendaciones personalizadas      | Autenticado         |
| `GET`    | `/articles/{id}/related`    | Artículos relacionados              | Autenticado         |
| `GET`    | `/articles/search?keyword=` | Búsqueda por título                 | Autenticado         |
| `POST`   | `/articles/upload`          | Publicar artículo (multipart)       | `REPORTER`, `ADMIN` |
| `GET`    | `/articles/my-articles`     | Artículos del autor con filtros     | `REPORTER`, `ADMIN` |
| `PATCH`  | `/articles/{id}`            | Actualizar campos del artículo      | `REPORTER`, `ADMIN` |
| `DELETE` | `/articles/{id}`            | Eliminar artículo                   | `REPORTER`, `ADMIN` |
| `GET`    | `/articles/reporter/{id}`   | Artículos de un reporter            | Autenticado         |

### 10.4 Categorías (`/api/categories`)

| Método | Endpoint           | Descripción       | Rol mínimo  |
| ------ | ------------------ | ----------------- | ----------- |
| `GET`  | `/categories`      | Listar categorías | Autenticado |
| `GET`  | `/categories/{id}` | Categoría por ID  | Autenticado |

### 10.5 Comentarios (`/api/comments`)

| Método   | Endpoint                 | Descripción                          | Rol mínimo                       |
| -------- | ------------------------ | ------------------------------------ | -------------------------------- |
| `GET`    | `/comments/article/{id}` | Comentarios paginados de un artículo | Autenticado                      |
| `POST`   | `/comments`              | Crear comentario con valoración      | Autenticado                      |
| `DELETE` | `/comments/{id}`         | Eliminar comentario                  | Autenticado (con permisos)       |
| `POST`   | `/comments/{id}/report`  | Reportar comentario                  | Autenticado                      |
| `GET`    | `/comments/reported`     | Comentarios reportados con filtros   | `MODERATOR`, `REPORTER`, `ADMIN` |
| `POST`   | `/comments/{id}/approve` | Aprobar comentario                   | `MODERATOR`, `ADMIN`             |
| `DELETE` | `/comments/{id}/reject`  | Eliminar comentario reportado        | `MODERATOR`, `ADMIN`             |

### 10.6 Moderación (`/api/moderators`)

| Método   | Endpoint                        | Descripción                          | Rol mínimo            |
| -------- | ------------------------------- | ------------------------------------ | --------------------- |
| `GET`    | `/moderators/my-requests`       | Solicitudes del reporter             | `REPORTER`            |
| `GET`    | `/moderators/pending`           | Solicitudes pendientes del moderador | `MODERATOR`, `READER` |
| `POST`   | `/moderators/request`           | Enviar solicitud                     | `REPORTER`            |
| `DELETE` | `/moderators/{id}`              | Cancelar solicitud                   | `REPORTER`            |
| `PUT`    | `/moderators/{id}/accept`       | Aceptar solicitud                    | `MODERATOR`           |
| `PUT`    | `/moderators/{id}/reject`       | Rechazar solicitud                   | `MODERATOR`           |
| `GET`    | `/moderators/is-moderator/{id}` | ¿Es moderador del reporter?          | Autenticado           |

### 10.7 Reporters (`/api/reporter`)

| Método | Endpoint         | Descripción                 | Rol mínimo  |
| ------ | ---------------- | --------------------------- | ----------- |
| `GET`  | `/reporter/{id}` | Perfil público del reporter | Autenticado |

---

## 11. Gestión de errores

La API devuelve siempre un objeto `ErrorResponse` estructurado cuando ocurre un error:

```json
{
  "timestamp": "2025-03-15T10:45:00",
  "status": 404,
  "error": "Not Found",
  "message": "Artículo no encontrado con ID: 99",
  "path": "/api/articles/99"
}
```

### 11.1 Catálogo de excepciones

| Excepción                     | HTTP  | Cuándo se lanza                                      |
| ----------------------------- | ----- | ---------------------------------------------------- |
| `AccountDisabledException`    | `403` | El usuario tiene `isActive = false` al hacer login.  |
| `BadRequestException`         | `400` | Petición semánticamente incorrecta.                  |
| `DuplicateResourceException`  | `409` | Recurso duplicado (distinto de usuario).             |
| `ExternalServiceException`    | `502` | Error al comunicarse con Cloudinary o Google OAuth2. |
| `ForbiddenAccessException`    | `403` | El usuario no tiene permiso para la operación.       |
| `InvalidCredentialsException` | `401` | Credenciales incorrectas en el login.                |
| `ResourceNotFoundException`   | `404` | El recurso no existe en la base de datos.            |
| `UserAlreadyExistsException`  | `409` | Email o username ya registrado.                      |

### 11.2 Errores de validación (400)

Los errores de validación de bean (`@NotBlank`, `@Size`, `@Email`, etc.) devuelven un `400 Bad Request` con los detalles de los campos que han fallado. Estos errores los genera automáticamente Spring Boot.

---

## 12. Mantenimiento y operaciones

### 12.1 Logs

La aplicación usa el sistema de logging estándar de Spring Boot (Logback). Los eventos relevantes registrados son:

- `INFO` — Subidas de vídeo exitosas a Cloudinary.
- `ERROR` — Fallos en la eliminación asíncrona de vídeos de Cloudinary.

### 12.2 Tareas periódicas recomendadas

- Revisar los logs de `CloudinaryService` para detectar vídeos huérfanos (artículos eliminados cuyo vídeo no se borró por error).
- Verificar usuarios con `isActive = false` para limpiar cuentas antiguas si es necesario.
- Revisar comentarios con `reviewed = false` en `comment_report` que lleven mucho tiempo sin procesar.

### 12.3 Tablas principales de la base de datos

| Tabla                | Descripción                                                           |
| -------------------- | --------------------------------------------------------------------- |
| `user`               | Usuarios del sistema. `isActive` controla el acceso.                  |
| `article`            | Artículos publicados. `isHidden` controla la visibilidad.             |
| `comment`            | Comentarios. `offenseCount` se incrementa con cada reporte.           |
| `comment_report`     | Registros de reporte. Restricción única `(comment_id, user_id)`.      |
| `category`           | Categorías disponibles. Campo `name` único.                           |
| `article_category`   | Relación N:N artículo ↔ categoría.                                    |
| `user_category`      | Categorías preferidas del usuario.                                    |
| `moderator_reporter` | Solicitudes de moderación con estado `PENDING`/`ACCEPTED`/`REJECTED`. |
| `user_block`         | Registros de bloqueo de cuentas con motivo y fecha de expiración.     |

### 12.4 Procesamiento asíncrono

La anotación `@EnableAsync` en `BackendTvCanariaApplication` habilita el procesamiento en segundo plano. Actualmente se usa para la eliminación de vídeos de Cloudinary. Para añadir más tareas asíncronas, anota el método con `@Async`.

### 12.5 CORS en producción

> **⛔ Importante:** Antes de desplegar en producción, actualiza la lista de orígenes permitidos en `CorsConfig.java` con los dominios reales del frontend. Dejar orígenes `localhost` habilitados en producción es un riesgo de seguridad.

[⬅ Volver al README Principal](../README.md)
