# sprngboot-mongodb
本项目展示了 SpringBoot 整合mongoDB进行文章存储，并实现简单增删改查，关于docker安装MongoDB：https://blog.csdn.net/MICHAELKING1/article/details/106121297。
一.创建新的springboot项目，引入pom文件。
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.7.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.dyz</groupId>
    <artifactId>mongodb</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>mongodb</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>

        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.3.3</version>
        </dependency>



        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
二、编写yml文件
spring:
  data:
    mongodb:
      host: 192.168.25.6
      port: 27017
      database: articleDB
logging:
  level:
    org.springframework.data.mongodb.core: debug
三、添加文章实体类
/**
 * @author dyz
 * @version 1.0
 * @date 2020/5/14 15:46
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    @Id
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 点赞数量
     */
    private Long thumbUp;

    /**
     * 访客数量
     */
    private Long visits;

}
四、添加Dao接口并继承MongoRepository
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
五、启动类添加snowflake，实现雪花算法，生成唯一ID
@SpringBootApplication
public class MongodbApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongodbApplication.class, args);
    }

    @Bean
    public Snowflake snowflake() {
        return IdUtil.createSnowflake(1, 1);
    }


}
六、编写测试类。
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
七、mongoDB可视化工具使用的是mongodb-compass，下载地址：https://www.mongodb.com/download-center/compass?jmp=docs 


八、源码地址：https://github.com/MichaelDYZ/springboot-mongodb
