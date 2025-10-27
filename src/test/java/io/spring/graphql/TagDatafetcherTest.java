package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class TagDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private TagsQueryService tagsQueryService;

  @BeforeEach
  public void setUp() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "testing");
    when(tagsQueryService.allTags()).thenReturn(tags);
  }

  @Test
  public void should_query_all_tags() {
    String query = "{ tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags", Collections.emptyMap());
    assertNotNull(result);
    assertEquals(4, result.size());
    assertTrue(result.contains("java"));
    assertTrue(result.contains("spring"));
    assertTrue(result.contains("graphql"));
    assertTrue(result.contains("testing"));
  }

  @Test
  public void should_return_empty_list_when_no_tags() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList());

    String query = "{ tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags", Collections.emptyMap());
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
