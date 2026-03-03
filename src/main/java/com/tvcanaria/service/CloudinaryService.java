package com.tvcanaria.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tvcanaria.entity.Article;
import com.tvcanaria.repository.ArticleRepository;

import jakarta.transaction.Transactional;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final ArticleRepository articleRepository;

    public CloudinaryService(Cloudinary cloudinary, ArticleRepository articleRepository) {
        this.cloudinary = cloudinary;
        this.articleRepository = articleRepository;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadVideo(MultipartFile file) throws IOException {
        return (Map<String, Object>) cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("resource_type", "video"));
    }

    @Transactional
    public void deleteVideoFromArticle(Integer id) {
        Article article = articleRepository.getReferenceById(id);

        if (article.getVideoUrl() == null) {
            throw new RuntimeException("El artículo no tiene un video asociado en la nube.");
        }

        try {
            cloudinary.uploader().destroy(
                    article.getVideoUrl(),
                    ObjectUtils.asMap("resource_type", "video"));

            articleRepository.deleteById(id);

        } catch (IOException e) {
            throw new RuntimeException("Error al conectar con Cloudinary para eliminar el archivo.");
        }
    }
}
