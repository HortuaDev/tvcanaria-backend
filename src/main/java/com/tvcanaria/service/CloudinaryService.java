package com.tvcanaria.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tvcanaria.entity.Article;

import jakarta.transaction.Transactional;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final ArticleService articleService;

    public CloudinaryService(Cloudinary cloudinary, ArticleService articleService) {
        this.cloudinary = cloudinary;
        this.articleService = articleService;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadVideo(MultipartFile file) throws IOException {
        return (Map<String, Object>) cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("resource_type", "video"));
    }

    @Transactional
    public void deleteVideoFromArticle(Integer id) {
        Article article = articleService.getArticleById(id);

        if (article.getVideoUrl() == null) {
            throw new RuntimeException("El artículo no tiene un video asociado en la nube.");
        }

        try {
            cloudinary.uploader().destroy(
                    article.getVideoUrl(),
                    ObjectUtils.asMap("resource_type", "video")
            );

            articleService.deleteArticle(id);
            
        } catch (IOException e) {
            throw new RuntimeException("Error al conectar con Cloudinary para eliminar el archivo.");
        }
    }
}
