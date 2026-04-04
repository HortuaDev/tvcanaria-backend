# Repositorios

**Paquete:** `com.tvcanaria.repository`

Todos los repositorios extienden `JpaRepository` y usan Spring Data JPA para el acceso a datos. Se detallan a continuación los métodos más relevantes de cada uno.

---

## ArticleRepository

**Entidad:** `Article`

| Método                                                 | Descripción                                                                                 |
| ------------------------------------------------------ | ------------------------------------------------------------------------------------------- |
| `findAll(Pageable)`                                    | Todos los artículos paginados, con `author` y `categories` cargados mediante `@EntityGraph` |
| `findByIsHiddenFalse(Pageable)`                        | Solo artículos visibles, paginados                                                          |
| `findByCategoriesCategoryId(Integer, Pageable)`        | Artículos de una categoría concreta, paginados                                              |
| `calculateAverageByArticleId(Integer)`                 | Calcula la valoración media del artículo (consulta nativa; ver nota abajo)                  |
| `findTop20ByIsHiddenFalseOrderByCreatedAtDesc()`       | Últimos 20 artículos visibles                                                               |
| `findTop20DistinctByCategoriesIn...`                   | Últimos 20 artículos de un conjunto de categorías                                           |
| `findRelatedArticles(categories, articleId, Pageable)` | Artículos relacionados por categoría, excluyendo el artículo actual                         |
| `findFallbackRelatedArticles(articleId, Pageable)`     | Artículos recientes como fallback si no hay relacionados                                    |
| `searchVisibleArticlesByTitle(keyword)`                | Búsqueda de artículos visibles por título (case-insensitive, `LIKE`)                        |
| `findByAuthorUserId(Integer)`                          | Artículos de un autor concreto                                                              |
| `findMyArticlesWithFilters(...)`                       | Búsqueda filtrada por usuario/rol, fecha, categorías y palabra clave                        |

### Nota: `calculateAverageByArticleId`

Consulta SQL nativa que implementa la lógica de valoración:

- Considera **el comentario más reciente de cada usuario** sobre el artículo.
- Solo incluye valoraciones `>= 0.5`.
- Devuelve el promedio redondeado al `0.5` más cercano usando `ROUND(AVG * 2) / 2`.
- Si no hay valoraciones, devuelve `0`.

### Nota: `findMyArticlesWithFilters`

Permite filtrar artículos con los siguientes parámetros:

| Parámetro       | Descripción                                              |
| --------------- | -------------------------------------------------------- |
| `userId`        | ID del usuario solicitante                               |
| `role`          | Rol del usuario (`ADMIN` ve todos; otros solo los suyos) |
| `dateFrom`      | Fecha mínima de creación (opcional)                      |
| `dateTo`        | Fecha máxima de creación (opcional)                      |
| `hasCategories` | Si se aplica filtro de categorías                        |
| `categories`    | Lista de nombres de categorías                           |
| `keyword`       | Palabra clave en el título (opcional)                    |

---

## CategoryRepository

**Entidad:** `Category`

Solo hereda los métodos estándar de `JpaRepository` (`findAll`, `findById`, `save`, `deleteById`, `existsById`).

---

## CommentRepository

**Entidad:** `Comment`

| Método                                                                               | Descripción                                                                            |
| ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------- |
| `findByArticle_ArticleIdOrderByCreatedAtDesc(Integer, Pageable)`                     | Comentarios de un artículo, ordenados por fecha descendente                            |
| `findReportedCommentsWithFilters(dateFrom, dateTo, Pageable)`                        | Comentarios con `offenseCount >= 1`, filtrado por fechas (para admins)                 |
| `findReportedCommentsByModeratorId(moderatorId, status, dateFrom, dateTo, Pageable)` | Comentarios reportados dentro de los artículos de los reporters asignados al moderador |
| `findReportedCommentsByReporterId(reporterId, dateFrom, dateTo, Pageable)`           | Comentarios reportados en artículos de un reporter concreto                            |

---

## CommentReportRepository

**Entidad:** `CommentReport`

| Método                                                          | Descripción                                      |
| --------------------------------------------------------------- | ------------------------------------------------ |
| `existsByComment_CommentIdAndReporter_UserId(Integer, Integer)` | Comprueba si un usuario ya reportó un comentario |
| `countByComment_CommentId(Integer)`                             | Número de reportes de un comentario              |
| `findByReviewedFalse()`                                         | Reportes aún no revisados                        |
| `findByComment_CommentId(Integer)`                              | Todos los reportes de un comentario concreto     |
| `deleteByComment_CommentId(Integer)`                            | Elimina todos los reportes de un comentario      |

---

## ModeratorReporterRepository

**Entidad:** `ModeratorReporter`

| Método                                                       | Descripción                                     |
| ------------------------------------------------------------ | ----------------------------------------------- |
| `findByModerator_UserIdAndStatus(Integer, Status)`           | Relaciones de un moderador filtradas por estado |
| `findByReporter_UserId(Integer)`                             | Todas las relaciones de un reporter             |
| `findByModerator_UserIdAndReporter_UserId(Integer, Integer)` | Relación específica entre moderador y reporter  |
| `findAcceptedModeratorsByReporter(reporterId, status)`       | Moderadores aceptados de un reporter            |
| `findAcceptedReportersByModerator(moderatorId, status)`      | Reporters aceptados de un moderador             |

---

## UserBlockRepository

**Entidad:** `UserBlock`

| Método                        | Descripción                                    |
| ----------------------------- | ---------------------------------------------- |
| `countByUser_UserId(Integer)` | Número de bloqueos registrados para un usuario |

---

## UserRepository

**Entidad:** `User`

| Método                                                    | Descripción                                                |
| --------------------------------------------------------- | ---------------------------------------------------------- |
| `findByEmail(String)`                                     | Busca usuario por email                                    |
| `findByUsername(String)`                                  | Busca usuario por username                                 |
| `findByEmailOrUsername(String, String)`                   | Busca por email o username (login)                         |
| `findByUserId(Integer)`                                   | Busca por ID                                               |
| `existsByEmail(String)`                                   | Comprueba si el email ya existe                            |
| `existsByUsername(String)`                                | Comprueba si el username ya existe                         |
| `findByProviderId(String)`                                | Busca usuario OAuth2 por ID externo                        |
| `searchUsersByKeyword(String)`                            | Búsqueda de usuarios por username, firstName o lastName    |
| `searchAndFilterUsers(query, dateFrom, dateTo, Pageable)` | Búsqueda paginada de usuarios con filtros de texto y fecha |

[⬅ Volver al README Principal](../README.md)
