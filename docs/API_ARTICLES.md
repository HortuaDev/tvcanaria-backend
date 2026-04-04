# API — Artículos

Este módulo gestiona la publicación, consulta, edición y eliminación de artículos (noticias en vídeo).

> Todos los endpoints requieren autenticación JWT salvo que se indique lo contrario.

---

## DTOs principales

### `ArticleRequest` — Crear artículo (JSON)

```json
{
  "title": "Incendio en Las Palmas",
  "description": "Descripción detallada del suceso...",
  "videoUrl": "https://res.cloudinary.com/.../video.mp4",
  "location": "Las Palmas de Gran Canaria",
  "categories": [1, 3]
}
```

| Campo         | Reglas                                    |
| ------------- | ----------------------------------------- |
| `title`       | Obligatorio · máx. 150 caracteres         |
| `description` | Opcional                                  |
| `videoUrl`    | Opcional · máx. 255 caracteres            |
| `location`    | Opcional · máx. 100 caracteres            |
| `categories`  | Obligatorio · al menos un ID de categoría |

---

### `ArticleUploadRequest` — Crear artículo con fichero de vídeo (multipart/form-data)

| Campo         | Reglas                                           |
| ------------- | ------------------------------------------------ |
| `title`       | Obligatorio · máx. 150 caracteres                |
| `description` | Opcional                                         |
| `location`    | Obligatorio · máx. 100 caracteres                |
| `categories`  | Obligatorio · al menos un ID                     |
| `video`       | Obligatorio · fichero de vídeo (`MultipartFile`) |

El vídeo se sube a Cloudinary y la URL resultante se almacena en el artículo.

---

### `ArticleUpdateRequest` — Actualizar artículo (PATCH)

Todos los campos son opcionales; solo se actualizan los que se envíen.

```json
{
  "title": "Nuevo título",
  "location": "Santa Cruz de Tenerife",
  "description": "Nueva descripción",
  "categoryIds": [2],
  "isHidden": true
}
```

| Campo         | Reglas                                     |
| ------------- | ------------------------------------------ |
| `title`       | Opcional · máx. 150 caracteres             |
| `location`    | Opcional · máx. 100 caracteres             |
| `description` | Opcional                                   |
| `categoryIds` | Opcional · lista de IDs                    |
| `isHidden`    | Opcional · `true` para ocultar el artículo |

---

### `ArticleResponse` — Respuesta

```json
{
  "articleId": 42,
  "title": "Incendio en Las Palmas",
  "description": "...",
  "videoUrl": "https://res.cloudinary.com/...",
  "isHidden": false,
  "location": "Las Palmas",
  "createdAt": "2024-03-15T10:30:00",
  "authorId": 7,
  "authorUsername": "reportero1",
  "rating": 4.5,
  "categories": [{ "categoryId": 1, "name": "Sucesos" }]
}
```

---

## Cálculo de valoración

La valoración (`rating`) de un artículo se calcula a partir de los comentarios usando la siguiente lógica:

- Se considera **el comentario más reciente de cada usuario** sobre ese artículo.
- Solo se incluyen valoraciones `>= 0.5`.
- El resultado es el **promedio redondeado al 0.5 más cercano**.

Esta lógica está implementada como consulta nativa en `ArticleRepository.calculateAverageByArticleId`.

---

## Consultas disponibles en el repositorio

| Método                                                 | Descripción                                            |
| ------------------------------------------------------ | ------------------------------------------------------ |
| `findAll(pageable)`                                    | Todos los artículos paginados (con autor y categorías) |
| `findByIsHiddenFalse(pageable)`                        | Solo artículos visibles, paginados                     |
| `findByCategoriesCategoryId(id, pageable)`             | Artículos de una categoría concreta                    |
| `findTop20ByIsHiddenFalseOrderByCreatedAtDesc()`       | Últimos 20 artículos visibles                          |
| `findTop20DistinctByCategoriesIn...`                   | Últimos 20 de un conjunto de categorías                |
| `findRelatedArticles(categories, articleId, pageable)` | Artículos relacionados por categoría                   |
| `findFallbackRelatedArticles(articleId, pageable)`     | Fallback si no hay relacionados                        |
| `searchVisibleArticlesByTitle(keyword)`                | Búsqueda por título (case-insensitive)                 |
| `findByAuthorUserId(userId)`                           | Artículos de un autor concreto                         |
| `findMyArticlesWithFilters(...)`                       | Búsqueda con filtros de fecha, categoría y rol         |

---

## Notas de implementación

- Los artículos eliminados también eliminan sus comentarios en cascada (`CascadeType.ALL`, `orphanRemoval = true`).
- La subida de vídeo a Cloudinary se realiza antes de persistir el artículo. Si falla, se lanza `ExternalServiceException`.
- La eliminación de vídeos de Cloudinary se hace **de forma asíncrona** (método `@Async` en `CloudinaryService`) para no bloquear la respuesta.

[⬅ Volver al README Principal](../README.md)
