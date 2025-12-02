package io.spring.graphql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.user.UserService;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = {DgsAutoConfiguration.class, TagDatafetcher.class})
@Import({TestSecurityConfig.class})
public class TagDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

    @MockBean private TagsQueryService tagsQueryService;

    // MockBeans for other DgsComponents discovered by DGS auto-configuration
    @MockBean private ArticleQueryService articleQueryService;
    @MockBean private ArticleCommandService articleCommandService;
    @MockBean private ArticleRepository articleRepository;
    @MockBean private ArticleFavoriteRepository articleFavoriteRepository;
    @MockBean private CommentQueryService commentQueryService;
    @MockBean private CommentRepository commentRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private UserQueryService userQueryService;
    @MockBean private JwtService jwtService;
    @MockBean private ProfileQueryService profileQueryService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private UserService userService;

    @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_all_tags() {
    List<String> tags = Arrays.asList("java", "spring", "graphql", "testing");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "query { tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result.size(), equalTo(4));
    assertThat(result.get(0), equalTo("java"));
    assertThat(result.get(1), equalTo("spring"));
    assertThat(result.get(2), equalTo("graphql"));
    assertThat(result.get(3), equalTo("testing"));
  }

  @Test
  public void should_get_empty_tags_list() {
    when(tagsQueryService.allTags()).thenReturn(Collections.emptyList());

    String query = "query { tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result.size(), equalTo(0));
  }

  @Test
  public void should_get_single_tag() {
    List<String> tags = Arrays.asList("java");
    when(tagsQueryService.allTags()).thenReturn(tags);

    String query = "query { tags }";

    List<String> result = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.tags");

    assertThat(result.size(), equalTo(1));
    assertThat(result.get(0), equalTo("java"));
  }
}
