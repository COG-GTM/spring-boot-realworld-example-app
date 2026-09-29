package io.spring.application;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converts free-form user input into a safe SQLite FTS5 MATCH expression.
 *
 * <p>The input is split on any character that is not a letter or digit, so FTS5 operators and
 * punctuation (quotes, {@code *}, {@code :}, {@code -}, {@code AND}/{@code OR} keywords, ...) can
 * never alter the query structure. Each remaining term is quoted and turned into a prefix query
 * ({@code "term"*}) and terms are combined with an implicit AND, so every term must match.
 */
public final class ArticleSearchQuery {
  public static final int MAX_TERMS = 10;

  private ArticleSearchQuery() {}

  public static Optional<String> toMatchExpression(String rawQuery) {
    if (rawQuery == null) {
      return Optional.empty();
    }
    String expression =
        Arrays.stream(rawQuery.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
            .filter(term -> !term.isEmpty())
            .distinct()
            .limit(MAX_TERMS)
            .map(term -> "\"" + term + "\"*")
            .collect(Collectors.joining(" "));
    return expression.isEmpty() ? Optional.empty() : Optional.of(expression);
  }
}
