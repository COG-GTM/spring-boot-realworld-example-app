package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentPayload;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class CommentDatafetcherTest {

  @Test
  void should_get_comment_from_local_context() {
    io.spring.application.CommentQueryService commentQueryService =
        mock(io.spring.application.CommentQueryService.class);
    CommentDatafetcher fetcher = new CommentDatafetcher(commentQueryService);

    CommentData commentData =
        new CommentData(
            "id1",
            "comment body",
            "articleId",
            new DateTime(),
            new DateTime(),
            new ProfileData("userId", "username", "bio", "image", false));

    com.netflix.graphql.dgs.DgsDataFetchingEnvironment dfe =
        mock(com.netflix.graphql.dgs.DgsDataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(commentData);

    var result = fetcher.getComment(dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("id1", result.getData().getId());
    assertEquals("comment body", result.getData().getBody());
  }
}
