package com.dyz.mongodb.dao;

import com.dyz.mongodb.data.Article;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @author dyz
 * @version 1.0
 * @date 2020/5/14 15:54
 */
public interface ArticleDao extends MongoRepository<Article, Long> {

    /**
     * 根据标题模糊查询
     *
     * @param title 标题
     * @return 满足条件的文章列表
     */
    List<Article> findByTitleLike(String title);

}
