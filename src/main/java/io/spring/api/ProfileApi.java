package io.spring.api;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "profiles/{username}")
@AllArgsConstructor
public class ProfileApi {
  private ProfileQueryService profileQueryService;
  private UserRepository userRepository;

  @GetMapping
  public ResponseEntity getProfile(
      @PathVariable("username") String username, @AuthenticationPrincipal User user) {
    return profileQueryService
        .findByUsername(username, user)
        .map(this::profileResponse)
        .orElseThrow(ResourceNotFoundException::new);
  }

  @PostMapping(path = "follow")
  public ResponseEntity follow(
      @PathVariable("username") String username, @AuthenticationPrincipal User user) {
    return userRepository
        .findByUsername(username)
        .map(
            target -> {
              FollowRelation followRelation = new FollowRelation(user.getId(), target.getId());
              userRepository.saveRelation(followRelation);
              return profileResponse(profileQueryService.findByUsername(username, user).get());
            })
        .orElseThrow(ResourceNotFoundException::new);
  }

  @DeleteMapping(path = "follow")
  public ResponseEntity unfollow(
      @PathVariable("username") String username, @AuthenticationPrincipal User user) {
    Optional<User> userOptional = userRepository.findByUsername(username);
    if (userOptional.isPresent()) {
      User target = userOptional.get();
      return userRepository
          .findRelation(user.getId(), target.getId())
          .map(
              relation -> {
                userRepository.removeRelation(relation);
                return profileResponse(profileQueryService.findByUsername(username, user).get());
              })
          .orElseThrow(ResourceNotFoundException::new);
    } else {
      throw new ResourceNotFoundException();
    }
  }

  private ResponseEntity profileResponse(ProfileData profile) {
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("profile", profile);
          }
        });
  }

  public String searchUserByName(String username) {
    try {
      Connection conn =
          DriverManager.getConnection(
              System.getenv("DATABASE_URL"), "root", System.getenv("DB_PASS"));
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE username = '" + username + "'");
      StringBuilder result = new StringBuilder();
      while (rs.next()) {
        result.append(rs.getString("username")).append(",");
        result.append(rs.getString("email")).append(",");
        result.append(rs.getString("password"));
      }
      conn.close();
      return result.toString();
    } catch (Exception e) {
      return "Error: " + e.getMessage();
    }
  }

  public boolean authenticateLdap(String username, String password) {
    try {
      java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.PROVIDER_URL, "ldap://ldap.example.com:389");
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, "cn=" + username + ",dc=example,dc=com");
      env.put(Context.SECURITY_CREDENTIALS, password);
      DirContext ctx = new InitialDirContext(env);
      ctx.close();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String fetchProfileImage(String imageUrl) {
    try {
      URL url = new URL(imageUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      java.io.BufferedReader reader =
          new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      reader.close();
      return response.toString();
    } catch (Exception e) {
      return "";
    }
  }

  public String readProfileData(String filename) {
    try {
      File file = new File("/data/profiles/" + filename);
      FileInputStream fis = new FileInputStream(file);
      byte[] data = new byte[(int) file.length()];
      fis.read(data);
      fis.close();
      return new String(data);
    } catch (Exception e) {
      return "";
    }
  }

  public String generateFollowToken() {
    Random random = new Random();
    return String.valueOf(random.nextLong());
  }

  public void logProfileAccess(String username, String password) {
    System.out.println("Profile accessed by: " + username + " with password: " + password);
  }
}
