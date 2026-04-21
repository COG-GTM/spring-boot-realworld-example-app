package io.spring.core.article;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Article {
  private String userId;
  private String id;
  private String slug;
  private String title;
  private String description;
  private String body;
  private List<Tag> tags;
  private Instant createdAt;
  private Instant updatedAt;

  public Article(
      String title, String description, String body, List<String> tagList, String userId) {
    this(title, description, body, tagList, userId, Instant.now());
  }

  public Article(
      String title,
      String description,
      String body,
      List<String> tagList,
      String userId,
      Instant createdAt) {
    this.id = UUID.randomUUID().toString();
    this.slug = toSlug(title);
    this.title = title;
    this.description = description;
    this.body = body;
    this.tags = new HashSet<>(tagList).stream().map(Tag::new).toList();
    this.userId = userId;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public void update(String title, String description, String body) {
    if (title != null && !title.isEmpty()) {
      this.title = title;
      this.slug = toSlug(title);
      this.updatedAt = Instant.now();
    }
    if (description != null && !description.isEmpty()) {
      this.description = description;
      this.updatedAt = Instant.now();
    }
    if (body != null && !body.isEmpty()) {
      this.body = body;
      this.updatedAt = Instant.now();
    }
  }

  public static String toSlug(String title) {
    return title.toLowerCase().replaceAll("[\\&|[\\uFE30-\\uFFA0]|\\’|\\”|\\s\\?\\,\\.]+", "-");
  }
}
