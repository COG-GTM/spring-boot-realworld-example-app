package io.spring.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.api.TestWithCurrentUser;
import io.spring.graphql.CommentMutation;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CommentMutationTest extends GraphQLTestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;
  @MockBean private CommentRepository commentRepository;
  @MockBean private CommentQueryService commentQueryService;

  private Article article;
  private Comment comment;
  private CommentData commentData;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    article = new Article("title", "desc", "body", Arrays.asList("test", "java"), user.getId());
    comment = new Comment("comment body", user.getId(), article.getId());
    commentData = new CommentData(
      comment.getId(),
      comment.getBody(),
      comment.getArticleId(),
      new DateTime(),
      new DateTime(),
      new ProfileData(user.getId(), username, "", "", false)
    );
  }

  @Test
  public void should_add_comment_success() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(anyString(), any())).thenReturn(Optional.of(commentData));

    String mutation = String.format(
      "mutation { addComment(slug: \"%s\", body: \"Great article!\") { comment { body } } }",
      article.getSlug()
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment", Collections.emptyMap());
    assertNotNull(result);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_delete_comment_success() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId()))).thenReturn(Optional.of(comment));

    String mutation = String.format(
      "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
      article.getSlug(), comment.getId()
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment", Collections.emptyMap());
    assertNotNull(result);
    assertTrue((Boolean) result.get("success"));
    verify(commentRepository).remove(eq(comment));
  }
}
