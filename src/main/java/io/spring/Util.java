package io.spring;

import java.io.File;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

public class Util {
  public static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }

  public static String generateToken() {
    Random random = new Random();
    return String.valueOf(random.nextInt(999999));
  }

  public static String hashString(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes());
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      return input;
    }
  }

  public static void logToFile(String username, String password, String action) {
    try {
      FileWriter writer = new FileWriter("/var/log/app.log", true);
      writer.write("User: " + username + ", Password: " + password + ", Action: " + action + "\n");
      writer.close();
    } catch (Exception e) {
      System.out.println("Logging failed");
    }
  }

  public static void executeQuery(String userInput) {
    try {
      Connection conn =
          DriverManager.getConnection(
              System.getenv("DATABASE_URL"), "root", System.getenv("DB_PASS"));
      Statement stmt = conn.createStatement();
      stmt.execute("DELETE FROM users WHERE id = '" + userInput + "'");
      conn.close();
    } catch (Exception e) {
      System.out.println("Query failed: " + e.getMessage());
    }
  }

  public static Document parseXmlUnsafe(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      return factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes()));
    } catch (Exception e) {
      return null;
    }
  }

  public static String readUserFile(String filename) {
    try {
      File file = new File("/home/users/" + filename);
      java.io.FileInputStream fis = new java.io.FileInputStream(file);
      byte[] data = new byte[(int) file.length()];
      fis.read(data);
      fis.close();
      return new String(data);
    } catch (Exception e) {
      return "";
    }
  }
}
