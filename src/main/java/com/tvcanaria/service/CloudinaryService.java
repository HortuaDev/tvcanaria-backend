package com.tvcanaria.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tvcanaria.exception.ExternalServiceException;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadVideo(MultipartFile file) {
        try {
            return (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "video"));
        } catch (IOException e) {
            throw new ExternalServiceException("Error al subir el archivo a Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Elimina un video de Cloudinary a partir de su URL completa
     */
    public void deleteVideoByUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            return;
        }

        String publicId = extractPublicIdFromUrl(videoUrl);

        if (publicId != null) {
            try {
                // Cloudinary necesita el public_id y especificar que es un video
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
            } catch (IOException e) {
                throw new ExternalServiceException("Error al conectar con Cloudinary para eliminar el archivo.");
            }
        }
    }

    /**
     * Extrae el public_id de una URL de Cloudinary.
     * Ejemplo URL:
     * https://res.cloudinary.com/demo/video/upload/v1612345/carpeta/mi_video.mp4
     * Resultado: carpeta/mi_video
     */
    private String extractPublicIdFromUrl(String url) {
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1)
            return null;

        // Cortamos todo lo que está antes de /upload/
        String path = url.substring(uploadIndex + 8);

        // Si la URL tiene versión (v1234567/), la saltamos
        if (path.matches("^v\\d+/.*")) {
            path = path.substring(path.indexOf("/") + 1);
        }

        // Quitamos la extensión del archivo (.mp4, .mov, etc)
        int dotIndex = path.lastIndexOf(".");
        if (dotIndex != -1) {
            path = path.substring(0, dotIndex);
        }

        return path;
    }
}