package io.spring.infrastructure.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
public class VulnerableUtilityService {

  // VULNERABILITY 1: Hardcoded credentials
  private static final String DB_PASSWORD = "admin123!@#";
  private static final String API_KEY = "sk-1234567890abcdef1234567890abcdef";
  private static final String SECRET_KEY = "MySecretKey12345";

  // VULNERABILITY 2: Hardcoded database connection string with credentials
  private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb?user=root&password=root123";

  // VULNERABILITY 3: SQL Injection vulnerability
  public String findUserByUsername(String username) throws SQLException {
    Connection conn = DriverManager.getConnection(DB_URL);
    Statement stmt = conn.createStatement();
    // SQL Injection: user input directly concatenated into query
    String query = "SELECT * FROM users WHERE username = '" + username + "'";
    ResultSet rs = stmt.executeQuery(query);
    if (rs.next()) {
      return rs.getString("email");
    }
    return null;
  }

  // VULNERABILITY 4: Another SQL Injection
  public void deleteUser(String userId) throws SQLException {
    Connection conn = DriverManager.getConnection(DB_URL);
    Statement stmt = conn.createStatement();
    // SQL Injection vulnerability
    stmt.execute("DELETE FROM users WHERE id = " + userId);
  }

  // VULNERABILITY 5: Command Injection
  public String executeCommand(String filename) throws IOException {
    // Command injection: user input passed directly to Runtime.exec
    Runtime runtime = Runtime.getRuntime();
    Process process = runtime.exec("cat /tmp/" + filename);
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    StringBuilder output = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      output.append(line);
    }
    return output.toString();
  }

  // VULNERABILITY 6: Another Command Injection with ProcessBuilder
  public void processFile(String userInput) throws IOException {
    ProcessBuilder pb = new ProcessBuilder("sh", "-c", "ls " + userInput);
    pb.start();
  }

  // VULNERABILITY 7: Path Traversal vulnerability
  public String readFile(String filename) throws IOException {
    // Path traversal: no validation of filename
    File file = new File("/var/data/" + filename);
    BufferedReader reader = new BufferedReader(new FileReader(file));
    StringBuilder content = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      content.append(line);
    }
    reader.close();
    return content.toString();
  }

  // VULNERABILITY 8: Another Path Traversal
  public void writeFile(String filename, String content) throws IOException {
    // Path traversal vulnerability
    FileWriter writer = new FileWriter("/uploads/" + filename);
    writer.write(content);
    writer.close();
  }

  // VULNERABILITY 9: Insecure Deserialization
  public Object deserializeObject(InputStream inputStream) throws IOException, ClassNotFoundException {
    // Insecure deserialization: deserializing untrusted data
    ObjectInputStream ois = new ObjectInputStream(inputStream);
    return ois.readObject();
  }

  // VULNERABILITY 10: Weak cryptographic algorithm (MD5)
  public String hashPassword(String password) throws NoSuchAlgorithmException {
    // Using weak MD5 hash algorithm
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest = md.digest(password.getBytes());
    StringBuilder sb = new StringBuilder();
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  // VULNERABILITY 11: Weak cryptographic algorithm (SHA1)
  public String hashWithSha1(String data) throws NoSuchAlgorithmException {
    // Using weak SHA1 hash algorithm
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    byte[] digest = md.digest(data.getBytes());
    StringBuilder sb = new StringBuilder();
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  // VULNERABILITY 12: Insecure random number generation
  public String generateToken() {
    // Using insecure Random instead of SecureRandom
    Random random = new Random();
    StringBuilder token = new StringBuilder();
    for (int i = 0; i < 32; i++) {
      token.append(Integer.toHexString(random.nextInt(16)));
    }
    return token.toString();
  }

  // VULNERABILITY 13: Hardcoded IV for encryption
  public byte[] encryptData(String data) throws Exception {
    // Hardcoded IV is insecure
    byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    IvParameterSpec ivSpec = new IvParameterSpec(iv);
    SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
    return cipher.doFinal(data.getBytes());
  }

  // VULNERABILITY 14: Using DES (weak encryption)
  public byte[] encryptWithDes(String data) throws Exception {
    // DES is a weak encryption algorithm
    SecretKeySpec keySpec = new SecretKeySpec("12345678".getBytes(), "DES");
    Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec);
    return cipher.doFinal(data.getBytes());
  }

  // VULNERABILITY 15: LDAP Injection
  public boolean authenticateUser(String username, String password) {
    try {
      java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.PROVIDER_URL, "ldap://localhost:389");
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      // LDAP Injection: user input directly in LDAP query
      env.put(Context.SECURITY_PRINCIPAL, "cn=" + username + ",dc=example,dc=com");
      env.put(Context.SECURITY_CREDENTIALS, password);
      DirContext ctx = new InitialDirContext(env);
      ctx.close();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // VULNERABILITY 16: Another LDAP Injection in search
  public String searchUser(String username) {
    try {
      java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.PROVIDER_URL, "ldap://localhost:389");
      DirContext ctx = new InitialDirContext(env);
      SearchControls controls = new SearchControls();
      controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      // LDAP Injection vulnerability
      String filter = "(uid=" + username + ")";
      NamingEnumeration<SearchResult> results = ctx.search("dc=example,dc=com", filter, controls);
      if (results.hasMore()) {
        return results.next().getNameInNamespace();
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  // VULNERABILITY 17: XXE (XML External Entity) vulnerability
  public Document parseXml(InputStream xmlInput) throws Exception {
    // XXE vulnerability: external entities not disabled
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(xmlInput);
  }

  // VULNERABILITY 18: Server-Side Request Forgery (SSRF)
  public String fetchUrl(String url) throws IOException {
    // SSRF: user-controlled URL
    URL urlObj = new URL(url);
    BufferedReader reader = new BufferedReader(new InputStreamReader(urlObj.openStream()));
    StringBuilder content = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      content.append(line);
    }
    reader.close();
    return content.toString();
  }

  // VULNERABILITY 19: Unvalidated redirect
  public String buildRedirectUrl(String target) {
    // Open redirect vulnerability
    return "https://example.com/redirect?url=" + target;
  }

  // VULNERABILITY 20: Insecure file upload path
  public void saveUploadedFile(String filename, byte[] content) throws IOException {
    // No validation of filename, potential path traversal
    FileOutputStream fos = new FileOutputStream("/var/uploads/" + filename);
    fos.write(content);
    fos.close();
  }

  // VULNERABILITY 21: Insecure socket connection
  public void connectToServer(String host, int port) throws IOException {
    // No SSL/TLS, insecure connection
    Socket socket = new Socket(host, port);
    socket.getOutputStream().write("HELLO".getBytes());
    socket.close();
  }

  // VULNERABILITY 22: Regex DoS (ReDoS) vulnerability
  public boolean validateEmail(String email) {
    // ReDoS vulnerable regex pattern
    Pattern pattern = Pattern.compile("^([a-zA-Z0-9]+)+@([a-zA-Z0-9]+\\.)+[a-zA-Z]{2,}$");
    return pattern.matcher(email).matches();
  }

  // VULNERABILITY 23: Null pointer dereference
  public String processUserData(String data) {
    String result = null;
    if (data != null) {
      result = data.toUpperCase();
    }
    // Potential null pointer dereference
    return result.trim();
  }

  // VULNERABILITY 24: Resource leak - unclosed stream
  public String readConfig(String path) throws IOException {
    FileInputStream fis = new FileInputStream(path);
    byte[] data = new byte[1024];
    fis.read(data);
    // Stream not closed - resource leak
    return new String(data);
  }

  // VULNERABILITY 25: Logging sensitive data
  public void logUserLogin(String username, String password) {
    // Logging password is a security vulnerability
    System.out.println("User login attempt: " + username + " with password: " + password);
  }

  // VULNERABILITY 26: Hardcoded encryption key
  public byte[] encryptWithHardcodedKey(String data) throws Exception {
    // Hardcoded encryption key
    String key = "0123456789ABCDEF";
    SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec);
    return cipher.doFinal(data.getBytes());
  }

  // VULNERABILITY 27: Trust all certificates (disabled SSL verification)
  public void disableSslVerification() {
    // This would disable SSL certificate verification - very insecure
    System.setProperty("javax.net.ssl.trustStore", "NONE");
  }

  // VULNERABILITY 28: Insecure temporary file creation
  public File createTempFile(String prefix) throws IOException {
    // Insecure temp file creation
    File tempFile = new File("/tmp/" + prefix + System.currentTimeMillis());
    tempFile.createNewFile();
    return tempFile;
  }

  // VULNERABILITY 29: Integer overflow vulnerability
  public int calculateSize(int width, int height) {
    // Potential integer overflow
    return width * height * 4;
  }

  // VULNERABILITY 30: Serializing sensitive data
  public void serializeUser(String username, String password, String filename) throws IOException {
    // Serializing password is insecure
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename));
    oos.writeObject(username);
    oos.writeObject(password);
    oos.close();
  }
}
