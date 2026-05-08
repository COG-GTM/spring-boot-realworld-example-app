package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.types.Comment;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment innerDfe;
  @InjectMocks private CommentDatafetcher datafetcher;
  private DgsDataFetchingEnvironment dfe;

  @BeforeEach
  public void setUp() {
    dfe = new DgsDataFetchingEnvironment(innerDfe);
  }

  @Test
  public void should_get_comment_from_local_context() {
    CommentData comment =
        new CommentData(
            "comment-id",
            "body",
            "article-id",
            new DateTime(),
            new DateTime(),
            new ProfileData("uid", "alice", "", "", false));
    when(innerDfe.getLocalContext()).thenReturn(comment);

    DataFetcherResult<Comment> result = datafetcher.getComment(dfe);

    assertNotNull(result.getData());
    assertEquals("comment-id", result.getData().getId());
    assertEquals("body", result.getData().getBody());
  }

  @Test
  public void should_throw_when_first_and_last_are_both_null() {
    assertThrows(
        IllegalArgumentException.class,
        () -> datafetcher.articleComments(null, null, null, null, dfe));
  }
}
