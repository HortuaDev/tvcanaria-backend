package com.tvcanaria.service;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tvcanaria.exception.ExternalServiceException;

/**
 * Servicio para la gestión de vídeos en Cloudinary.
 */
@Service
public class CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Sube un archivo de vídeo a Cloudinary.
     *
     * @param file archivo de vídeo a subir
     * @return mapa con los metadatos de la subida (incluye {@code secure_url}, {@code public_id}, etc.)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadVideo(MultipartFile file) {
        try {
            return (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "video"));
        } catch (IOException e) {
            throw new ExternalServiceException("Error al subir el archivo a Cloudinary: Inténtalo de nuevo más tarde.");
        }
    }

    /**
     * Elimina un vídeo de Cloudinary de forma asíncrona a partir de su URL.
     * Los errores se registran en el log pero no interrumpen la ejecución.
     *
     * @param videoUrl URL completa del vídeo en Cloudinary
     */
    @Async
    public void deleteVideoByUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            return;
        }

        String publicId = extractPublicIdFromUrl(videoUrl);

        if (publicId != null) {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
                logger.info("Vídeo eliminado exitosamente de Cloudinary en segundo plano: {}", publicId);
            } catch (IOException e) {
                logger.error("Error asíncrono al intentar eliminar el vídeo en Cloudinary: {}", videoUrl, e);
            }
        }
    }

    /**
     * Extrae el {@code public_id} de una URL de Cloudinary eliminando la versión y la extensión.
     * Ejemplo: {@code https://res.cloudinary.com/demo/video/upload/v1612345/carpeta/mi_video.mp4}
     * → {@code carpeta/mi_video}
     *
     * @param url URL completa del recurso en Cloudinary
     * @return el {@code public_id} extraído, o {@code null} si la URL no tiene el formato esperado
     */
    private String extractPublicIdFromUrl(String url) {
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1)
            return null;

        String path = url.substring(uploadIndex + 8);

        if (path.matches("^v\\d+/.*")) {
            path = path.substring(path.indexOf("/") + 1);
        }

        int dotIndex = path.lastIndexOf(".");
        if (dotIndex != -1) {
            path = path.substring(0, dotIndex);
        }

        return path;
    }
}