package io.spring.application;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converts free-form user input into a safe SQLite FTS5 MATCH expression.
 *
 * <p>The input is split on any character that is not a letter, combining mark or digit, so FTS5
 * operators and punctuation (quotes, {@code *}, {@code :}, {@code -}, parentheses, ...) can never
 * alter the query structure. Each remaining term is quoted, which also neutralises the {@code
 * AND}/{@code OR}/{@code NOT} keywords, and turned into a prefix query ({@code "term"*}); terms are
 * combined with an implicit AND, so every term must match. Case folding and diacritic removal are
 * left to the FTS5 tokenizer so queries are normalised exactly like the indexed text.
 */
public final class ArticleSearchQuery {
  public static final int MAX_QUERY_LENGTH = 256;
  public static final int MAX_TERMS = 10;

  private ArticleSearchQuery() {}

  public static Optional<String> toMatchExpression(String rawQuery) {
    if (rawQuery == null) {
      return Optional.empty();
    }
    String bounded =
        rawQuery.length() > MAX_QUERY_LENGTH ? rawQuery.substring(0, MAX_QUERY_LENGTH) : rawQuery;
    String expression =
        Arrays.stream(
                Normalizer.normalize(bounded, Normalizer.Form.NFC).split("[^\\p{L}\\p{M}\\p{N}]+"))
            .filter(term -> !term.isEmpty())
            .distinct()
            .limit(MAX_TERMS)
            .map(term -> "\"" + term + "\"*")
            .collect(Collectors.joining(" "));
    return expression.isEmpty() ? Optional.empty() : Optional.of(expression);
  }
}
