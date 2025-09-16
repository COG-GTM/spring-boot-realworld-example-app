package io.spring.infrastructure;

import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@MybatisTest
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
    "SET FOREIGN_KEY_CHECKS = 0",
    "TRUNCATE TABLE article_tags",
    "TRUNCATE TABLE article_favorites", 
    "TRUNCATE TABLE comments",
    "TRUNCATE TABLE follows",
    "TRUNCATE TABLE articles",
    "TRUNCATE TABLE users",
    "TRUNCATE TABLE tags",
    "SET FOREIGN_KEY_CHECKS = 1"
})
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = {
    "SET FOREIGN_KEY_CHECKS = 0",
    "TRUNCATE TABLE article_tags",
    "TRUNCATE TABLE article_favorites", 
    "TRUNCATE TABLE comments",
    "TRUNCATE TABLE follows",
    "TRUNCATE TABLE articles",
    "TRUNCATE TABLE users",
    "TRUNCATE TABLE tags",
    "SET FOREIGN_KEY_CHECKS = 1"
})
public abstract class DbTestBase {}
