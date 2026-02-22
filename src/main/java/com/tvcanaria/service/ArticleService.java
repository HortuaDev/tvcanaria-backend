package com.tvcanaria.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.tvcanaria.dto.ArticleRequest;
import com.tvcanaria.dto.ArticleResponse;
import com.tvcanaria.entity.Article;
import com.tvcanaria.entity.User;
import com.tvcanaria.repository.ArticleRepository;
import com.tvcanaria.repository.UserRepository;

@Service
public class ArticleService {
    private ArticleRepository articleRepository;
    private UserRepository userRepository;

    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public ArticleResponse createArticle(ArticleRequest request, String authentication) {

        User user = userRepository.findByUsername(authentication)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setDescription(request.getDescription());
        article.setVideoUrl(request.getVideo_url());
        article.setLocation(request.getLocation());
        article.setIsHidden(false);
        article.setCreatedAt(LocalDateTime.now());
        article.setAuthor(user);

        articleRepository.save(article);

        return new ArticleResponse(article.getArticleId(), article.getTitle(), article.getDescription(),
                article.getVideoUrl(), article.getLocation(), authentication);
    }
}
