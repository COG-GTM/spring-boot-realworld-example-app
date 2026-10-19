package io.spring.application.comment;

import io.spring.application.CommentCursor;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.DbTestBase;
import io.spring.infrastructure.repository.MyBatisArticleRepository;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import io.spring.infrastructure.repository.MyBatisUserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  MyBatisCommentRepository.class,
  MyBatisUserRepository.class,
  CommentQueryService.class,
  MyBatisArticleRepository.class
})
public class CommentQueryServiceTest extends DbTestBase {
  @Autowired private CommentRepository commentRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private CommentQueryService commentQueryService;

  @Autowired private ArticleRepository articleRepository;

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("aisensiy@test.com", "aisensiy", "123", "", "");
    userRepository.save(user);
  }

  @Test
  public void should_read_comment_success() {
    Comment comment = new Comment("content", user.getId(), "123");
    commentRepository.save(comment);

    Optional<CommentData> optional = commentQueryService.findById(comment.getId(), user);
    Assertions.assertTrue(optional.isPresent());
    CommentData commentData = optional.get();
    Assertions.assertEquals(commentData.getProfileData().getUsername(), user.getUsername());
  }

  @Test
  public void should_read_comments_of_article() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    User user2 = new User("user2@email.com", "user2", "123", "", "");
    userRepository.save(user2);
    userRepository.saveRelation(new FollowRelation(user.getId(), user2.getId()));

    Comment comment1 = new Comment("content1", user.getId(), article.getId());
    commentRepository.save(comment1);
    Comment comment2 = new Comment("content2", user2.getId(), article.getId());
    commentRepository.save(comment2);

    List<CommentData> comments = commentQueryService.findByArticleId(article.getId(), user);
    Assertions.assertEquals(comments.size(), 2);
  }

  @Test
  public void should_limit_comments_of_article_by_cursor_page_size() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    List<String> newestFirst = new ArrayList<>();
    long base = new DateTime().minusHours(1).getMillis();
    try {
      for (int i = 0; i < 5; i++) {
        DateTimeUtils.setCurrentMillisFixed(base + i * 1000L);
        Comment comment = new Comment("content" + i, user.getId(), article.getId());
        commentRepository.save(comment);
        newestFirst.add(0, comment.getId());
      }
    } finally {
      DateTimeUtils.setCurrentMillisSystem();
    }

    CursorPager<CommentData> first =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(), user, new CursorPageParameter<>(null, 2, Direction.NEXT));
    Assertions.assertEquals(newestFirst.subList(0, 2), ids(first));
    Assertions.assertTrue(first.hasNext());

    CursorPager<CommentData> second =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(),
            user,
            new CursorPageParameter<>(
                CommentCursor.parse(first.getEndCursor().toString()), 2, Direction.NEXT));
    Assertions.assertEquals(newestFirst.subList(2, 4), ids(second));
    Assertions.assertTrue(second.hasNext());

    CursorPager<CommentData> third =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(),
            user,
            new CursorPageParameter<>(
                CommentCursor.parse(second.getEndCursor().toString()), 2, Direction.NEXT));
    Assertions.assertEquals(newestFirst.subList(4, 5), ids(third));
    Assertions.assertFalse(third.hasNext());

    CursorPager<CommentData> last =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(), user, new CursorPageParameter<>(null, 2, Direction.PREV));
    Assertions.assertEquals(newestFirst.subList(3, 5), ids(last));
    Assertions.assertTrue(last.hasPrevious());
  }

  @Test
  public void should_page_through_comments_sharing_the_same_timestamp() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleRepository.save(article);

    List<String> created = new ArrayList<>();
    try {
      DateTimeUtils.setCurrentMillisFixed(new DateTime().minusHours(1).getMillis());
      for (int i = 0; i < 4; i++) {
        Comment comment = new Comment("content" + i, user.getId(), article.getId());
        commentRepository.save(comment);
        created.add(comment.getId());
      }
    } finally {
      DateTimeUtils.setCurrentMillisSystem();
    }
    List<String> idsDesc =
        created.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());

    CursorPager<CommentData> first =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(), user, new CursorPageParameter<>(null, 2, Direction.NEXT));
    Assertions.assertEquals(idsDesc.subList(0, 2), ids(first));
    Assertions.assertTrue(first.hasNext());

    CursorPager<CommentData> second =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(),
            user,
            new CursorPageParameter<>(
                CommentCursor.parse(first.getEndCursor().toString()), 2, Direction.NEXT));
    Assertions.assertEquals(idsDesc.subList(2, 4), ids(second));
    Assertions.assertFalse(second.hasNext());

    CursorPager<CommentData> before =
        commentQueryService.findByArticleIdWithCursor(
            article.getId(),
            user,
            new CursorPageParameter<>(
                CommentCursor.parse(second.getStartCursor().toString()), 2, Direction.PREV));
    Assertions.assertEquals(idsDesc.subList(0, 2), ids(before));
    Assertions.assertFalse(before.hasPrevious());
  }

  private static List<String> ids(CursorPager<CommentData> pager) {
    return pager.getData().stream().map(CommentData::getId).collect(Collectors.toList());
  }
}
