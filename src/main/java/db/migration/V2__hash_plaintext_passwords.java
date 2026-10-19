package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Re-hashes passwords that were stored in plaintext by the profile update path before it encoded
 * them, so no cleartext credential stays in the users table.
 */
public class V2__hash_plaintext_passwords extends BaseJavaMigration {
  private static final Pattern BCRYPT_HASH =
      Pattern.compile("\\A\\$2(a|y|b)?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}");

  @Override
  public void migrate(Context context) throws Exception {
    hashPlaintextPasswords(context.getConnection(), new BCryptPasswordEncoder());
  }

  static int hashPlaintextPasswords(Connection connection, PasswordEncoder encoder)
      throws SQLException {
    Map<String, String> plaintext = new LinkedHashMap<>();
    try (PreparedStatement select =
            connection.prepareStatement(
                "select id, password from users where password is not null and password <> ''");
        ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        String password = rs.getString("password");
        if (!BCRYPT_HASH.matcher(password).matches()) {
          plaintext.put(rs.getString("id"), password);
        }
      }
    }
    try (PreparedStatement update =
        connection.prepareStatement("update users set password = ? where id = ?")) {
      for (Map.Entry<String, String> entry : plaintext.entrySet()) {
        update.setString(1, encoder.encode(entry.getValue()));
        update.setString(2, entry.getKey());
        update.addBatch();
      }
      update.executeBatch();
    }
    return plaintext.size();
  }
}
