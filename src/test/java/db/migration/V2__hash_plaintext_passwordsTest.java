package db.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class V2__hash_plaintext_passwordsTest {
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private Connection connection;

  @BeforeEach
  public void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "create table users (id varchar(255) primary key, username varchar(255),"
              + " password varchar(255), email varchar(255), bio text, image varchar(511))");
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    connection.close();
  }

  @Test
  public void should_hash_plaintext_passwords_and_keep_existing_hashes() throws Exception {
    String existingHash = encoder.encode("registered");
    insert("plain", "newpw");
    insert("hashed", existingHash);
    insert("empty", "");

    int migrated = V2__hash_plaintext_passwords.hashPlaintextPasswords(connection, encoder);

    assertEquals(1, migrated);
    String rehashed = passwordOf("plain");
    assertNotEquals("newpw", rehashed);
    assertTrue(encoder.matches("newpw", rehashed));
    assertEquals(existingHash, passwordOf("hashed"));
    assertEquals("", passwordOf("empty"));
  }

  private void insert(String id, String password) throws Exception {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "insert into users (id, username, password) values (?, ?, ?)")) {
      statement.setString(1, id);
      statement.setString(2, id);
      statement.setString(3, password);
      statement.executeUpdate();
    }
  }

  private String passwordOf(String id) throws Exception {
    try (PreparedStatement statement =
        connection.prepareStatement("select password from users where id = ?")) {
      statement.setString(1, id);
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        return rs.getString(1);
      }
    }
  }
}
