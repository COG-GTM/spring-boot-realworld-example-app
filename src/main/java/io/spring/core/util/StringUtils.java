package io.spring.core.util;

/**
 * Utility class for common string operations used across the application. Centralizes string
 * manipulation logic for consistency and reusability.
 */
public final class StringUtils {

  private StringUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if a string is null or empty.
   *
   * @param value the string to check
   * @return true if the string is null or empty, false otherwise
   */
  public static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }

  /**
   * Checks if a string is not null and not empty.
   *
   * @param value the string to check
   * @return true if the string is not null and not empty, false otherwise
   */
  public static boolean isNotEmpty(String value) {
    return !isEmpty(value);
  }

  /**
   * Converts a title string to a URL-friendly slug. Replaces special characters, spaces, and
   * unicode characters with hyphens.
   *
   * @param title the title to convert
   * @return the slug representation of the title
   */
  public static String toSlug(String title) {
    if (title == null) {
      return "";
    }
    String slug =
        title.toLowerCase().replaceAll("[\\&|[\\uFE30-\\uFFA0]|\\'|\\\"|\\s\\?\\,\\.]+", "-");
    return cleanupSlug(slug);
  }

  /**
   * Cleans up a slug by removing leading/trailing whitespace and normalizing. This ensures slugs
   * are properly formatted for URL usage.
   *
   * @param slug the slug to clean up
   * @return the cleaned slug
   */
  private static String cleanupSlug(String slug) {
    // Trim whitespace from the slug
    return slug.trim();
  }
}
