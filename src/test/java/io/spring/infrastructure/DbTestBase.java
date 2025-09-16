package io.spring.infrastructure;

import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@MybatisTest
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = {
    "DELETE FROM article_tags",
    "DELETE FROM article_favorites", 
    "DELETE FROM comments",
    "DELETE FROM follows",
    "DELETE FROM articles",
    "DELETE FROM users",
    "DELETE FROM tags"
})
public abstract class DbTestBase {}
