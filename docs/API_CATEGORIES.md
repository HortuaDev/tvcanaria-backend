# API — Categorías

**Base path:** `/api/categories`

Las categorías son etiquetas temáticas que se asignan a los artículos y a las preferencias de los usuarios.

---

## GET `/api/categories`

Devuelve todas las categorías disponibles en el sistema.

### Response `200 OK`

```json
[
  { "categoryId": 1, "name": "Sucesos" },
  { "categoryId": 2, "name": "Deportes" },
  { "categoryId": 3, "name": "Cultura" }
]
```

---

## GET `/api/categories/{id}`

Obtiene una categoría concreta por su ID.

### Path param

| Parámetro | Tipo      | Descripción        |
| --------- | --------- | ------------------ |
| `id`      | `Integer` | ID de la categoría |

### Response `200 OK`

```json
{ "categoryId": 1, "name": "Sucesos" }
```

### Errores posibles

| Código | Causa                                                 |
| ------ | ----------------------------------------------------- |
| `404`  | Categoría no encontrada (`ResourceNotFoundException`) |

---

## DTO: `CategoryResponse`

```java
Integer categoryId   // Identificador único
String name          // Nombre de la categoría
```

---

## Gestión interna (CategoryService)

Además de los endpoints públicos, `CategoryService` expone métodos para uso interno del sistema:

### `getCategoriesByIds(Set<Integer> ids)`

Resuelve un conjunto de IDs a entidades `Category`. Si algún ID no existe, lanza `ResourceNotFoundException` indicando cuáles no se encontraron.

Utilizado en la creación y actualización de artículos para validar las categorías asignadas.

### `updateCategory(Integer id, Category data)`

Actualiza el nombre de una categoría existente. Solo el campo `name` es actualizable.

### `deleteCategory(Integer id)`

Elimina una categoría. Lanza `ResourceNotFoundException` si el ID no existe.

---

## `UserCategoryRequest`

DTO usado para asignar o actualizar las categorías de interés de un usuario.

```json
{
  "categoryIds": [1, 3, 5]
}
```

| Campo         | Regla                         |
| ------------- | ----------------------------- |
| `categoryIds` | Conjunto de IDs de categorías |

[⬅ Volver al README Principal](../README.md)
