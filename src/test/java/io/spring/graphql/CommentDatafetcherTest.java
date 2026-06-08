package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.graphql.types.Comment;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;

  @InjectMocks private CommentDatafetcher commentDatafetcher;

  @Test
  void getComment_should_return_comment_from_local_context() {
    ProfileData profileData = new ProfileData("userId", "username", "bio", "img", false);
    CommentData commentData =
        new CommentData(
            "comment1", "comment body", "article1", new DateTime(), new DateTime(), profileData);

    DataFetchingEnvironment mockDfe = mock(DataFetchingEnvironment.class);
    when(mockDfe.getLocalContext()).thenReturn(commentData);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(mockDfe);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertNotNull(result);
    assertEquals("comment1", result.getData().getId());
    assertEquals("comment body", result.getData().getBody());
  }
}
