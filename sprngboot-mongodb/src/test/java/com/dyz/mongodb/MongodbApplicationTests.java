package com.dyz.mongodb;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.dyz.mongodb.dao.ArticleDao;
import com.dyz.mongodb.data.Article;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
class MongodbApplicationTests {

    @Autowired
    private ArticleDao articleDao;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Snowflake snowflake;

    /**
     * 新增数据
     */
    @Test
    public void testSave() {
        Article article = new Article(1L, RandomUtil.randomString(20), RandomUtil.randomString(150), DateUtil.date(), DateUtil
                .date(), 0L, 0L);
        articleDao.save(article);
        log.info("【文章】= {}", JSONUtil.toJsonStr(article));
    }

    /**
     * 新增数据列表
     */
    @Test
    public void testSaveList() {
        List<Article> articles = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            articles.add(new Article(snowflake.nextId(), RandomUtil.randomString(20), RandomUtil.randomString(150), DateUtil
                    .date(), DateUtil.date(), 0L, 0L));
        }
        articleDao.saveAll(articles);

        log.info("【文章】= {}", JSONUtil.toJsonStr(articles.stream()
                .map(Article::getId)
                .collect(Collectors.toList())));
    }

    /**
     * 更新数据
     */
    @Test
    public void testUpdate() {
        articleDao.findById(1262211396846882816L).ifPresent(article -> {
            article.setTitle(article.getTitle() + "这条数据被更新");
            article.setUpdateTime(DateUtil.date());
            articleDao.save(article);
            log.info("【文章】= {}", JSONUtil.toJsonStr(article));
        });
    }

    /**
     * 删除数据
     */
    @Test
    public void testDelete() {
        // 根据主键删除
        articleDao.deleteById(1L);

        // 全部删除
        articleDao.deleteAll();
    }

    /**
     * 增加点赞数、访客数，使用save方式更新点赞、访客
     */
    @Test
    public void testThumbUp() {
        articleDao.findById(1262211396846882816L).ifPresent(article -> {
            article.setThumbUp(article.getThumbUp() + 1);
            article.setVisits(article.getVisits() + 1);
            articleDao.save(article);
            log.info("【标题】= {}【点赞数】= {}【访客数】= {}", article.getTitle(), article.getThumbUp(), article.getVisits());
        });
    }

    /**
     * 增加点赞数、访客数，使用更优雅/高效的方式更新点赞、访客
     */
    @Test
    public void testThumbUp2() {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(1262211396846882816L));
        Update update = new Update();
        update.inc("thumbUp", 1L);
        update.inc("visits", 1L);
        mongoTemplate.updateFirst(query, update, "article");

        articleDao.findById(1262211396846882816L)
                .ifPresent(article -> log.info("【标题】= {}【点赞数】= {}【访客数】= {}", article.getTitle(), article.getThumbUp(), article
                        .getVisits()));
    }

    /**
     * 分页排序查询
     */
    @Test
    public void testQuery() {
        Sort sort = Sort.by("thumbUp", "updateTime").descending();
        PageRequest pageRequest = PageRequest.of(0, 5, sort);
        Page<Article> all = articleDao.findAll(pageRequest);
        log.info("【总页数】= {}", all.getTotalPages());
        log.info("【总条数】= {}", all.getTotalElements());
        log.info("【当前页数据】= {}", JSONUtil.toJsonStr(all.getContent()
                .stream()
                .map(article -> "文章标题：" + article.getTitle() + "点赞数：" + article.getThumbUp() + "更新时间：" + article.getUpdateTime())
                .collect(Collectors.toList())));
    }

    /**
     * 根据标题模糊查询
     */
    @Test
    public void testFindByTitleLike() {
        List<Article> articles = articleDao.findByTitleLike("更新");
        log.info("【文章】= {}", JSONUtil.toJsonStr(articles));
    }

}
