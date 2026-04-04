# Arquitectura y Modelo de Datos

---

## Estructura del proyecto

```
com.tvcanaria/
├── config/          # Configuraciones de beans (Cloudinary, CORS)
├── controller/      # Controladores REST
├── dto/             # Objetos de transferencia de datos
│   ├── article/
│   ├── auth/
│   ├── category/
│   ├── comment/
│   ├── error/
│   ├── moderator/
│   ├── profile/
│   └── user/
├── entity/          # Entidades JPA (modelo de base de datos)
├── enums/           # Enumeraciones del dominio
├── exception/       # Excepciones personalizadas
├── repository/      # Repositorios Spring Data JPA
├── security/        # Filtro JWT y proveedor de tokens
└── service/         # Lógica de negocio
```

---

## Entidades

### `User`

**Tabla:** `user`

Representa a cualquier usuario del sistema, independientemente de su rol.

| Campo          | Tipo                   | Descripción                                |
| -------------- | ---------------------- | ------------------------------------------ |
| `userId`       | `Integer` (PK)         | Identificador único                        |
| `authProvider` | `String` (20)          | `LOCAL` o `GOOGLE`                         |
| `providerId`   | `String` (100)         | ID externo (OAuth2)                        |
| `username`     | `String` (25, unique)  | Nombre de usuario                          |
| `firstName`    | `String` (50)          | Nombre                                     |
| `lastName`     | `String` (50)          | Apellidos                                  |
| `email`        | `String` (255, unique) | Correo electrónico                         |
| `passwordHash` | `String` (64)          | Contraseña hasheada (BCrypt)               |
| `role`         | `Role` (enum)          | Rol del usuario (defecto: `READER`)        |
| `isActive`     | `Boolean`              | Si la cuenta está activa (defecto: `true`) |
| `createdAt`    | `LocalDateTime`        | Fecha de registro (auto)                   |

**Relaciones:**

- `OneToMany` → `Article` (artículos publicados)
- `OneToMany` → `Comment` (comentarios realizados)
- `ManyToMany` → `Category` (categorías de interés)
- `OneToOne` → `UserBlock` (bloqueo activo, si existe)
- `OneToMany` → `ModeratorReporter` (relaciones de moderación)

---

### `Article`

**Tabla:** `article`

Contenido publicado por un reporter.

| Campo         | Tipo               | Descripción                                   |
| ------------- | ------------------ | --------------------------------------------- |
| `articleId`   | `Integer` (PK)     | Identificador único                           |
| `title`       | `String` (150)     | Título del artículo                           |
| `description` | `TEXT`             | Descripción opcional                          |
| `videoUrl`    | `String` (255)     | URL del vídeo en Cloudinary                   |
| `isHidden`    | `Boolean`          | Si el artículo está oculto (defecto: `false`) |
| `location`    | `String` (100)     | Localización geográfica                       |
| `createdAt`   | `LocalDateTime`    | Fecha de publicación (auto)                   |
| `rating`      | `BigDecimal` (3,1) | Valoración media calculada                    |

**Relaciones:**

- `ManyToOne` → `User` (autor)
- `OneToMany` → `Comment`
- `ManyToMany` → `Category`

---

### `Category`

**Tabla:** `category`

Etiqueta temática que agrupa artículos y preferencias de usuarios.

| Campo        | Tipo                  | Descripción            |
| ------------ | --------------------- | ---------------------- |
| `categoryId` | `Integer` (PK)        | Identificador único    |
| `name`       | `String` (50, unique) | Nombre de la categoría |

**Relaciones:**

- `ManyToMany` → `User`
- `ManyToMany` → `Article`

---

### `Comment`

**Tabla:** `comment`

Comentario y valoración de un usuario sobre un artículo.

| Campo          | Tipo               | Descripción                                 |
| -------------- | ------------------ | ------------------------------------------- |
| `commentId`    | `Integer` (PK)     | Identificador único                         |
| `comment`      | `TEXT`             | Texto del comentario                        |
| `rating`       | `BigDecimal` (2,1) | Valoración (0.5 – 5.0)                      |
| `offenseCount` | `Integer`          | Número de reportes recibidos (defecto: `0`) |
| `createdAt`    | `LocalDateTime`    | Fecha de creación (auto)                    |

**Relaciones:**

- `ManyToOne` → `Article`
- `ManyToOne` → `User` (autor del comentario)

---

### `CommentReport`

**Tabla:** `comment_report`

Registro de un reporte de un comentario inapropiado.

| Campo         | Tipo            | Descripción                                             |
| ------------- | --------------- | ------------------------------------------------------- |
| `reportId`    | `Integer` (PK)  | Identificador único                                     |
| `reviewed`    | `Boolean`       | Si ha sido revisado por un moderador (defecto: `false`) |
| `validReport` | `Boolean`       | Resultado de la revisión                                |
| `createdAt`   | `LocalDateTime` | Fecha del reporte (auto)                                |

**Restricción única:** combinación `(comment_id, user_id)` — un usuario solo puede reportar un comentario una vez.

**Relaciones:**

- `ManyToOne` → `Comment`
- `ManyToOne` → `User` (quien reporta)

---

### `ModeratorReporter`

**Tabla:** `moderator_reporter`

Vinculación entre un moderador y un reporter. El reporter solicita al moderador que supervise sus artículos.

| Campo       | Tipo            | Descripción                                 |
| ----------- | --------------- | ------------------------------------------- |
| `id`        | `Integer` (PK)  | Identificador único                         |
| `status`    | `Status` (enum) | Estado de la solicitud (defecto: `PENDING`) |
| `createdAt` | `LocalDateTime` | Fecha de solicitud (auto)                   |

**Relaciones:**

- `ManyToOne` → `User` (moderador)
- `ManyToOne` → `User` (reporter)

---

### `UserBlock`

**Tabla:** `user_block`

Registro de bloqueo de una cuenta de usuario.

| Campo          | Tipo            | Descripción                                |
| -------------- | --------------- | ------------------------------------------ |
| `blockId`      | `Integer` (PK)  | Identificador único                        |
| `reason`       | `String` (255)  | Motivo del bloqueo                         |
| `createdAt`    | `LocalDateTime` | Fecha del bloqueo (auto)                   |
| `blockedUntil` | `LocalDateTime` | Fecha de expiración del bloqueo (opcional) |

**Relaciones:**

- `OneToOne` → `User`

---

## Enumeraciones

### `Role`

| Valor       | Descripción                                   |
| ----------- | --------------------------------------------- |
| `READER`    | Usuario base, puede leer, comentar y valorar  |
| `REPORTER`  | Puede publicar artículos con vídeo            |
| `MODERATOR` | Modera comentarios de sus reporters asignados |
| `ADMIN`     | Acceso completo y administración del sistema  |

### `Status`

Usado en `ModeratorReporter` para el ciclo de vida de una solicitud de moderación.

| Valor      | Descripción                                     |
| ---------- | ----------------------------------------------- |
| `PENDING`  | Solicitud enviada, pendiente de respuesta       |
| `ACCEPTED` | El moderador ha aceptado supervisar al reporter |
| `REJECTED` | El moderador ha rechazado la solicitud          |

---

## Diagrama de relaciones (resumen)

```
User ──< Article >─── Category
 │          │
 │       Comment ──< CommentReport
 │
 ├── UserBlock
 └── ModeratorReporter (moderador ↔ reporter)
```

[⬅ Volver al README Principal](../README.md)
