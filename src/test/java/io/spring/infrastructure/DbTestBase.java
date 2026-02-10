package io.spring.infrastructure;

import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@MybatisTest
@Sql(
    statements = {
      "DELETE FROM article_favorites",
      "DELETE FROM article_tags",
      "DELETE FROM comments",
      "DELETE FROM follows",
      "DELETE FROM articles",
      "DELETE FROM tags",
      "DELETE FROM users"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class DbTestBase {}
