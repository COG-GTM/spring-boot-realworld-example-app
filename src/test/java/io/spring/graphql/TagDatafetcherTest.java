package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
public class TagDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private TagsQueryService tagsQueryService;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_all_tags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "dgs");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "{ tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assert result.size() == 4;
    assert result.contains("java");
    assert result.contains("spring");
    assert result.contains("graphql");
    assert result.contains("dgs");
  }

  @Test
  public void should_return_empty_tags_list() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    String query = "{ tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assert result.isEmpty();
  }

  @Test
  public void should_return_single_tag() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java"));

    String query = "{ tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assert result.size() == 1;
    assert result.get(0).equals("java");
  }
}
