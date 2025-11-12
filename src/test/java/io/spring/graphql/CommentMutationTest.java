package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;


@SpringBootTest(classes = {DgsAutoConfiguration.class, CommentMutation.class})
public class CommentMutationTest extends GraphQLTestBase {

  @Autowired DgsQueryExecutor dgsQueryExecutor;

  @MockBean ArticleRepository articleRepository;

  @MockBean CommentRepository commentRepository;

  @MockBean CommentQueryService commentQueryService;

  private Article article;
  private Comment comment;
  private CommentData commentData;

  @BeforeEach
  @Override
  public void setUpUser() {
    super.setUpUser();
    article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("tag1"), user.getId());
    comment = new Comment("Test comment body", user.getId(), article.getId());
    
    ProfileData profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    commentData = new CommentData(
        comment.getId(),
        comment.getBody(),
        article.getId(),
        new DateTime(),
        new DateTime(),
        profileData
    );
  }

  @Test
  
  public void testAddComment() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    String mutation = "mutation { addComment(slug: \"test-slug\", body: \"Test comment body\") { comment { id body } } }";
    
    assertNotNull(dgsQueryExecutor.executeAndGetDocumentContext(mutation));
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  
  public void testAddCommentToNonExistentArticle() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { addComment(slug: \"non-existent\", body: \"Test comment\") { comment { id } } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment");
    });
  }

  @Test
  
  public void testDeleteComment() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId()))).thenReturn(Optional.of(comment));

    String mutation = "mutation { deleteComment(slug: \"test-slug\", id: \"" + comment.getId() + "\") { success } }";
    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");
    
    assertTrue(success);
    verify(commentRepository).remove(comment);
  }

  @Test
  
  public void testDeleteCommentNotFound() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { deleteComment(slug: \"test-slug\", id: \"non-existent\") { success } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment");
    });
  }

  @Test
  
  public void testDeleteCommentFromNonExistentArticle() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    String mutation = "mutation { deleteComment(slug: \"non-existent\", id: \"comment-id\") { success } }";
    
    assertThrows(Exception.class, () -> {
      dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment");
    });
  }
}
