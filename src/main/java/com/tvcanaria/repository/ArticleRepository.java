package com.tvcanaria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tvcanaria.entity.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Integer> {

    List<Article> findByIsHiddenFalse();

    List<Article> findByCategoriesCategoryId(Integer categoryId);

}
