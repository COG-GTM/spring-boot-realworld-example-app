package io.spring.graphql;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.spring.application.CommentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;

  private CommentDatafetcher commentDatafetcher;

  @BeforeEach
  public void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
  }

  @Test
  public void should_create_datafetcher_instance() {
    assertThat(commentDatafetcher, notNullValue());
  }
}
