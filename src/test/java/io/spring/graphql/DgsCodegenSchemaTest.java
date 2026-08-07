package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.language.FieldDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.language.UnionTypeDefinition;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the Netflix DGS codegen output against the GraphQL schema. The codegen plugin is
 * Kotlin-based and runs in the Gradle JVM, so a toolchain change can silently stop producing
 * sources; these assertions fail loudly if the generated {@code io.spring.graphql.types} classes
 * stop matching {@code schema.graphqls}.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional
public class DgsCodegenSchemaTest {

  private static final String GENERATED_TYPES_PACKAGE = "io.spring.graphql.types";
  private static final Set<String> DEFAULT_ROOT_TYPES = Set.of("Query", "Mutation", "Subscription");

  @Autowired private DgsQueryExecutor dgsQueryExecutor;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private UserRepository userRepository;

  /**
   * Datafetchers read the security context directly, which over HTTP always holds at least an
   * anonymous token. In-process execution bypasses the filter chain, so populate it here.
   */
  @BeforeEach
  public void setUpAnonymousAuthentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "dgs-codegen-test",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  private static TypeDefinitionRegistry schema() throws IOException {
    try (InputStreamReader reader =
        new InputStreamReader(
            new ClassPathResource("schema/schema.graphqls").getInputStream(),
            StandardCharsets.UTF_8)) {
      return new SchemaParser().parse(reader);
    }
  }

  /** Operation roots have no generated type; read them from {@code schema { ... }} if declared. */
  private static Set<String> rootTypeNames(TypeDefinitionRegistry registry) {
    return registry
        .schemaDefinition()
        .map(
            definition ->
                definition.getOperationTypeDefinitions().stream()
                    .map(operation -> operation.getTypeName().getName())
                    .collect(Collectors.toSet()))
        .orElse(DEFAULT_ROOT_TYPES);
  }

  private static Class<?> generatedClass(String typeName) {
    try {
      return Class.forName(GENERATED_TYPES_PACKAGE + "." + typeName);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(
          "No generated class for schema type " + typeName + "; did generateJava run?", e);
    }
  }

  private static Set<String> declaredFieldNames(Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
        .filter(field -> !field.isSynthetic())
        .map(Field::getName)
        .collect(Collectors.toSet());
  }

  @Test
  public void everySchemaTypeHasAGeneratedClass() throws IOException {
    TypeDefinitionRegistry registry = schema();
    Set<String> rootTypes = rootTypeNames(registry);
    List<String> typeNames =
        registry.types().values().stream()
            .map(TypeDefinition::getName)
            .filter(name -> !rootTypes.contains(name))
            .collect(Collectors.toList());

    assertThat(typeNames).isNotEmpty();
    typeNames.forEach(DgsCodegenSchemaTest::generatedClass);
  }

  @Test
  public void generatedObjectTypesExposeEverySchemaField() throws IOException {
    TypeDefinitionRegistry registry = schema();
    Set<String> rootTypes = rootTypeNames(registry);
    for (TypeDefinition<?> type : registry.types().values()) {
      if (rootTypes.contains(type.getName())) {
        continue;
      }
      if (type instanceof ObjectTypeDefinition) {
        Set<String> generatedFields = declaredFieldNames(generatedClass(type.getName()));
        List<String> schemaFields =
            ((ObjectTypeDefinition) type)
                .getFieldDefinitions().stream()
                    .map(FieldDefinition::getName)
                    .collect(Collectors.toList());
        assertThat(generatedFields)
            .as("generated fields of %s", type.getName())
            .containsAll(schemaFields);
      } else if (type instanceof InputObjectTypeDefinition) {
        Set<String> generatedFields = declaredFieldNames(generatedClass(type.getName()));
        List<String> schemaFields =
            ((InputObjectTypeDefinition) type)
                .getInputValueDefinitions().stream()
                    .map(InputValueDefinition::getName)
                    .collect(Collectors.toList());
        assertThat(generatedFields)
            .as("generated fields of %s", type.getName())
            .containsAll(schemaFields);
      }
    }
  }

  @Test
  public void generatedUnionMembersImplementTheUnionInterface() throws IOException {
    for (TypeDefinition<?> type : schema().types().values()) {
      if (!(type instanceof UnionTypeDefinition)) {
        continue;
      }
      Class<?> unionClass = generatedClass(type.getName());
      assertThat(unionClass.isInterface()).as("%s is an interface", type.getName()).isTrue();
      ((UnionTypeDefinition) type)
          .getMemberTypes().stream()
              .map(graphql.language.TypeName.class::cast)
              .forEach(
                  member ->
                      assertThat(unionClass.isAssignableFrom(generatedClass(member.getName())))
                          .as("%s implements %s", member.getName(), type.getName())
                          .isTrue());
    }
  }

  @Test
  public void datafetchersResolveAgainstTheGeneratedSchema() {
    io.spring.core.user.User author =
        new io.spring.core.user.User(
            "dgs-codegen@example.com", "dgs-codegen-user", "password", "", "");
    userRepository.save(author);
    io.spring.core.article.Article seeded =
        new io.spring.core.article.Article(
            "DGS codegen round trip",
            "description",
            "body",
            List.of("dgs-codegen"),
            author.getId());
    articleRepository.save(seeded);

    // Deserializing into the generated Article proves the runtime response binds to the codegen
    // output field-for-field, not merely that the query executed without errors.
    Article article =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            String.format(
                "{ article(slug: \"%s\") { title slug description body tagList favorited"
                    + " favoritesCount } }",
                seeded.getSlug()),
            "data.article",
            Article.class);
    assertThat(article.getTitle()).isEqualTo("DGS codegen round trip");
    assertThat(article.getSlug()).isEqualTo(seeded.getSlug());
    assertThat(article.getBody()).isEqualTo("body");
    assertThat(article.getTagList()).containsExactly("dgs-codegen");
    assertThat(article.getFavoritesCount()).isZero();

    List<String> tags = dgsQueryExecutor.executeAndExtractJsonPath("{ tags }", "data.tags");
    assertThat(tags).contains("dgs-codegen");
  }
}
