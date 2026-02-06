package io.spring.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/vulnerable")
public class VulnerableApi {

  // Hardcoded credentials - VULNERABILITY
  private static final String ADMIN_PASSWORD = "SuperSecret123!";
  private static final String DB_CONNECTION = "jdbc:mysql://prod-db.internal:3306/users?user=admin&password=Pr0dP@ss!";
  private static final String AWS_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
  private static final String AWS_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

  // XSS Vulnerability 1: Reflected XSS
  @GetMapping("/search")
  public ResponseEntity<String> search(@RequestParam("q") String query) {
    // XSS: User input directly reflected in response without encoding
    String html = "<html><body><h1>Search Results for: " + query + "</h1></body></html>";
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }

  // XSS Vulnerability 2: Stored XSS simulation
  @PostMapping("/comment")
  public ResponseEntity<Map<String, String>> addComment(@RequestBody Map<String, String> body) {
    String comment = body.get("comment");
    // XSS: Storing and returning user input without sanitization
    Map<String, String> response = new HashMap<>();
    response.put("message", "Comment added: " + comment);
    response.put("html", "<div class='comment'>" + comment + "</div>");
    return ResponseEntity.ok(response);
  }

  // SQL Injection Vulnerability
  @GetMapping("/user/{id}")
  public ResponseEntity<Map<String, Object>> getUser(@PathVariable("id") String userId) throws SQLException {
    Connection conn = DriverManager.getConnection(DB_CONNECTION);
    Statement stmt = conn.createStatement();
    // SQL Injection: Direct string concatenation
    ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = '" + userId + "'");
    Map<String, Object> user = new HashMap<>();
    if (rs.next()) {
      user.put("id", rs.getString("id"));
      user.put("name", rs.getString("name"));
    }
    return ResponseEntity.ok(user);
  }

  // Path Traversal Vulnerability
  @GetMapping("/download")
  public ResponseEntity<byte[]> downloadFile(@RequestParam("file") String filename) throws IOException {
    // Path Traversal: No validation of filename
    Path filePath = Paths.get("/var/files/" + filename);
    byte[] content = Files.readAllBytes(filePath);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(content);
  }

  // Open Redirect Vulnerability
  @GetMapping("/redirect")
  public void redirect(@RequestParam("url") String url, HttpServletResponse response) throws IOException {
    // Open Redirect: No validation of redirect URL
    response.sendRedirect(url);
  }

  // Insecure Cookie - Missing HttpOnly and Secure flags
  @PostMapping("/login")
  public ResponseEntity<String> login(
      @RequestParam("username") String username,
      @RequestParam("password") String password,
      HttpServletResponse response) {
    // Insecure cookie configuration
    Cookie sessionCookie = new Cookie("session", generateSessionId());
    sessionCookie.setMaxAge(3600);
    // Missing: sessionCookie.setHttpOnly(true);
    // Missing: sessionCookie.setSecure(true);
    sessionCookie.setPath("/");
    response.addCookie(sessionCookie);
    
    // Logging sensitive data - VULNERABILITY
    System.out.println("Login attempt for user: " + username + " with password: " + password);
    
    return ResponseEntity.ok("Logged in");
  }

  // Insecure Random for session ID
  private String generateSessionId() {
    // Using insecure Random instead of SecureRandom
    Random random = new Random();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 32; i++) {
      sb.append(Integer.toHexString(random.nextInt(16)));
    }
    return sb.toString();
  }

  // SSRF Vulnerability
  @GetMapping("/fetch")
  public ResponseEntity<String> fetchExternalResource(@RequestParam("url") String url) throws IOException {
    // SSRF: User-controlled URL without validation
    URL resourceUrl = new URL(url);
    InputStream is = resourceUrl.openStream();
    byte[] content = is.readAllBytes();
    is.close();
    return ResponseEntity.ok(new String(content));
  }

  // Insecure Deserialization
  @PostMapping("/deserialize")
  public ResponseEntity<Object> deserialize(HttpServletRequest request) throws IOException, ClassNotFoundException {
    // Insecure deserialization of untrusted data
    ObjectInputStream ois = new ObjectInputStream(request.getInputStream());
    Object obj = ois.readObject();
    ois.close();
    return ResponseEntity.ok(obj);
  }

  // File Upload without validation
  @PostMapping("/upload")
  public ResponseEntity<String> uploadFile(
      @RequestParam("filename") String filename,
      @RequestBody byte[] content) throws IOException {
    // No file type validation, path traversal possible
    File uploadDir = new File("/var/uploads");
    File targetFile = new File(uploadDir, filename);
    FileOutputStream fos = new FileOutputStream(targetFile);
    fos.write(content);
    fos.close();
    return ResponseEntity.ok("File uploaded: " + filename);
  }

  // Information Disclosure - Stack trace in response
  @GetMapping("/error")
  public ResponseEntity<String> triggerError(@RequestParam("type") String errorType) {
    try {
      if ("null".equals(errorType)) {
        String s = null;
        s.length(); // NPE
      } else if ("divide".equals(errorType)) {
        int result = 10 / 0; // ArithmeticException
      }
    } catch (Exception e) {
      // Information disclosure: returning full stack trace
      return ResponseEntity.status(500).body("Error: " + e.toString() + "\nStack: " + java.util.Arrays.toString(e.getStackTrace()));
    }
    return ResponseEntity.ok("OK");
  }

  // Header Injection Vulnerability
  @GetMapping("/header")
  public ResponseEntity<String> setHeader(
      @RequestParam("name") String headerName,
      @RequestParam("value") String headerValue) {
    // Header injection: user-controlled header values
    return ResponseEntity.ok()
        .header(headerName, headerValue)
        .body("Header set");
  }

  // Weak password validation
  @PostMapping("/register")
  public ResponseEntity<String> register(
      @RequestParam("username") String username,
      @RequestParam("password") String password) {
    // Weak password policy - only checking length
    if (password.length() < 4) {
      return ResponseEntity.badRequest().body("Password too short");
    }
    // Storing password in plain text (simulated)
    System.out.println("Registered user: " + username + " with password: " + password);
    return ResponseEntity.ok("User registered");
  }

  // Timing attack vulnerability in password comparison
  @PostMapping("/verify")
  public ResponseEntity<Boolean> verifyPassword(
      @RequestParam("input") String inputPassword) {
    // Timing attack: using equals() instead of constant-time comparison
    boolean isValid = ADMIN_PASSWORD.equals(inputPassword);
    return ResponseEntity.ok(isValid);
  }

  // Resource leak - unclosed stream
  @GetMapping("/read")
  public ResponseEntity<String> readResource(@RequestParam("path") String path) throws IOException {
    FileInputStream fis = new FileInputStream(path);
    byte[] data = new byte[1024];
    fis.read(data);
    // Stream not closed - resource leak
    return ResponseEntity.ok(new String(data));
  }

  // Null pointer dereference
  @GetMapping("/process")
  public ResponseEntity<String> processData(@RequestParam(value = "data", required = false) String data) {
    String processed = null;
    if (data != null && !data.isEmpty()) {
      processed = data.toUpperCase();
    }
    // Potential NPE if data is null
    return ResponseEntity.ok(processed.trim());
  }
}
