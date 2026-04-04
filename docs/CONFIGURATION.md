# Configuración del Sistema

Este documento describe todas las configuraciones necesarias para poner en marcha el backend de TV Canaria.

---

## Cloudinary (`CloudinaryConfig`)

**Clase:** `com.tvcanaria.config.CloudinaryConfig`

Registra el bean de `Cloudinary` inyectando las credenciales desde `application.properties`.

### Propiedades requeridas

| Propiedad               | Descripción                    |
| ----------------------- | ------------------------------ |
| `cloudinary.cloud-name` | Nombre del cloud en Cloudinary |
| `cloudinary.api-key`    | API Key de la cuenta           |
| `cloudinary.api-secret` | API Secret de la cuenta        |

### Ejemplo

```properties
cloudinary.cloud-name=mi-cloud
cloudinary.api-key=123456789012345
cloudinary.api-secret=aBcDeFgHiJkLmNoPqRsTuVwXyZ
```

> Nunca incluir estas credenciales en el repositorio. Usar variables de entorno o un gestor de secretos.

---

## CORS (`CorsConfig`)

**Clase:** `com.tvcanaria.config.CorsConfig`

Configura los orígenes, métodos y cabeceras permitidos para las peticiones cross-origin.

### Orígenes permitidos

| Origen                   | Uso                          |
| ------------------------ | ---------------------------- |
| `http://localhost:19006` | Expo web                     |
| `http://localhost:8081`  | Metro bundler (React Native) |
| `http://localhost:3000`  | Puerto alternativo para web  |
| `exp://localhost:8081`   | Expo Go                      |
| `http://10.0.2.2:8082`   | Emulador Android             |

### Métodos HTTP permitidos

`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`

### Cabeceras permitidas

`Authorization`, `Content-Type`, `X-Requested-With`, `Accept`, `Origin`, `Access-Control-Request-Method`, `Access-Control-Request-Headers`

### Cabeceras expuestas al cliente

`Access-Control-Allow-Origin`, `Access-Control-Allow-Credentials`, `Authorization`

### Otras opciones

| Opción             | Valor                       |
| ------------------ | --------------------------- |
| `allowCredentials` | `true`                      |
| `maxAge`           | `3600` segundos             |
| Patrón de ruta     | `/**` (todos los endpoints) |

> **Nota para producción:** actualiza la lista de orígenes permitidos con los dominios reales del frontend antes de desplegar.

---

## JWT

**Clase:** `com.tvcanaria.security.JwtTokenProvider`

### Propiedades requeridas

| Propiedad        | Descripción                                                            |
| ---------------- | ---------------------------------------------------------------------- |
| `jwt.secret`     | Clave secreta para firmar los tokens. Mínimo 256 bits (32 caracteres). |
| `jwt.expiration` | Tiempo de expiración en milisegundos (ej. `86400000` = 24 horas)       |

### Ejemplo

```properties
jwt.secret=MiClaveSecretaMuyLargaYSeguraDeAlMenos32Chars!
jwt.expiration=86400000
```

Los tokens se firman con el algoritmo **HS256** e incluyen los claims: `sub` (userId), `email`, `username` y `role`.

---

## Procesamiento asíncrono

**Clase:** `com.tvcanaria.BackendTvCanariaApplication`

La aplicación tiene habilitado el procesamiento asíncrono mediante la anotación `@EnableAsync`. Esto permite que operaciones como la eliminación de vídeos en Cloudinary se ejecuten en segundo plano sin bloquear la respuesta al cliente.

[⬅ Volver al README Principal](../README.md)
