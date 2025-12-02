package io.spring.graphql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.user.UserService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, CommentMutation.class, CommentDatafetcher.class})
@Import({TestSecurityConfig.class})
public class CommentMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

    @MockBean private CommentQueryService commentQueryService;

    // MockBeans for other DgsComponents discovered by DGS auto-configuration
    @MockBean private ArticleQueryService articleQueryService;
    @MockBean private ArticleCommandService articleCommandService;
    @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private UserQueryService userQueryService;
    @MockBean private JwtService jwtService;
    @MockBean private ProfileQueryService profileQueryService;
    @MockBean private TagsQueryService tagsQueryService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private UserService userService;

    private User user;
  private User anotherUser;
  private Article article;
  private Comment comment;
  private CommentData commentData;

    @BeforeEach
    public void setUp() {
      user = new User("john@jacob.com", "johnjacob", "123", "", "");
      anotherUser = new User("another@test.com", "another", "123", "", "");
      article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("tag1"), user.getId());
      comment = new Comment("Test comment body", user.getId(), article.getId());
      commentData = createCommentData(comment, user);
      // Set up anonymous authentication to avoid NullPointerException in SecurityUtil.getCurrentUser()
      AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
          "anonymous", "anonymousUser", java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
      SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
    }

  private CommentData createCommentData(Comment comment, User author) {
    DateTime now = DateTime.now();
    return new CommentData(
        comment.getId(),
        comment.getBody(),
        article.getId(),
        now,
        now,
        new ProfileData(author.getId(), author.getUsername(), author.getBio(), author.getImage(), false));
  }

  @Test
  public void should_add_comment_success() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    String mutation =
        String.format(
            "mutation { addComment(slug: \"%s\", body: \"Test comment body\") { comment { id body } } }",
            article.getSlug());

    String body = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.addComment.comment.body");

    assertThat(body, equalTo("Test comment body"));

    verify(commentRepository, atLeastOnce()).save(any(Comment.class));
  }

  @Test
  public void should_fail_add_comment_without_authentication() {
    String mutation =
        String.format(
            "mutation { addComment(slug: \"%s\", body: \"Test comment\") { comment { id } } }",
            article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_add_comment_when_article_not_found() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { addComment(slug: \"nonexistent\", body: \"Test\") { comment { id } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_delete_comment_success() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId()))).thenReturn(Optional.of(comment));

    String mutation =
        String.format(
            "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
            article.getSlug(), comment.getId());

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");

    assertThat(success, equalTo(true));

    verify(commentRepository, atLeastOnce()).remove(any(Comment.class));
  }

  @Test
  public void should_delete_comment_as_article_author() {
    setAuthentication(user);
    Comment otherComment = new Comment("Other comment", anotherUser.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(otherComment.getId()))).thenReturn(Optional.of(otherComment));

    String mutation =
        String.format(
            "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
            article.getSlug(), otherComment.getId());

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteComment.success");

    assertThat(success, equalTo(true));

    verify(commentRepository, atLeastOnce()).remove(any(Comment.class));
  }

  @Test
  public void should_fail_delete_comment_when_not_authorized() {
    setAuthentication(anotherUser);
    Article otherArticle = new Article("Other", "Other", "Other", Arrays.asList(), anotherUser.getId());
    Article articleByOther = new Article("Title", "Desc", "Body", Arrays.asList(), "other-user-id");
    Comment commentByUser = new Comment("Comment", user.getId(), articleByOther.getId());
    
    when(articleRepository.findBySlug(eq(articleByOther.getSlug()))).thenReturn(Optional.of(articleByOther));
    when(commentRepository.findById(eq(articleByOther.getId()), eq(commentByUser.getId()))).thenReturn(Optional.of(commentByUser));

    String mutation =
        String.format(
            "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
            articleByOther.getSlug(), commentByUser.getId());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_delete_comment_when_article_not_found() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation = "mutation { deleteComment(slug: \"nonexistent\", id: \"123\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  @Test
  public void should_fail_delete_comment_when_comment_not_found() {
    setAuthentication(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        String.format(
            "mutation { deleteComment(slug: \"%s\", id: \"nonexistent\") { success } }",
            article.getSlug());

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors().isEmpty(), equalTo(false));
  }

  private void setAuthentication(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
