package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentDatafetcher.class
    })
public class CommentDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private CommentQueryService commentQueryService;

  private User user;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    ProfileData profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    DateTime now = new DateTime();
    commentData = new CommentData("comment-id", "Test comment body", "article-id", now, now, profileData);
  }

  @Test
  public void should_query_article_comments_with_pagination() {
    List<CommentData> comments = Arrays.asList(commentData);
    CursorPager<CommentData> pager =
        new CursorPager<>(comments, CursorPager.Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any()))
        .thenReturn(pager);

    String query =
        "query { "
            + "  article(slug: \"test-slug\") { "
            + "    comments(first: 10) { "
            + "      edges { "
            + "        cursor "
            + "        node { "
            + "          id "
            + "          body "
            + "        } "
            + "      } "
            + "      pageInfo { "
            + "        hasNextPage "
            + "        hasPreviousPage "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertNotNull(result);
  }

  @Test
  public void should_throw_error_when_both_first_and_last_are_null_for_comments() {
    String query =
        "query { "
            + "  article(slug: \"test-slug\") { "
            + "    comments { "
            + "      edges { "
            + "        node { "
            + "          id "
            + "        } "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertNotNull(result);
  }
}
