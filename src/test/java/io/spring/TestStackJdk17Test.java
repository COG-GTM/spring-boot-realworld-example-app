package io.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.bytebuddy.ClassFileVersion;
import org.junit.jupiter.api.Test;

/**
 * Guards the test stack itself against JDK strong encapsulation. Mockito mocks through byte-buddy
 * and rest-assured's JsonPath/XmlPath go through Groovy; both reach into areas the JDK
 * progressively locks down, so a JDK bump can break them while production code stays fine.
 */
class TestStackJdk17Test {

  @Test
  void byteBuddyShouldSupportTheRunningJvmWithoutExperimentalMode() {
    ClassFileVersion running = ClassFileVersion.ofThisVm();
    assertTrue(
        running.isAtLeast(ClassFileVersion.JAVA_V17),
        "expected to run on JDK 17+, but byte-buddy sees " + running);
    assertTrue(
        running.isAtMost(ClassFileVersion.latest()),
        "byte-buddy only supports this JVM in experimental mode; bump net.bytebuddy:byte-buddy");
  }

  @Test
  void mockitoShouldSubclassApplicationClassesCompiledForJava17() throws IOException {
    assertEquals(
        ClassFileVersion.JAVA_V17,
        ClassFileVersion.of(User.class),
        "test assumes application classes are compiled for Java 17");

    UserRepository userRepository = mock(UserRepository.class);
    User user = new User("john@jacob.com", "johnjacob", "123", "", "");
    when(userRepository.findByUsername(eq("johnjacob"))).thenReturn(Optional.of(user));

    assertEquals(Optional.of(user), userRepository.findByUsername("johnjacob"));
    verify(userRepository).findByUsername("johnjacob");
  }

  /**
   * GPath evaluation compiles the expression to Groovy and dispatches it reflectively, so it is the
   * part of rest-assured a JDK bump is most likely to break. The closure form is included because
   * it exercises Groovy's runtime call site machinery rather than plain property access.
   */
  @Test
  void restAssuredGroovyPathsShouldResolveUnderStrongEncapsulation() {
    JsonPath json = new JsonPath("{\"user\":{\"username\":\"johnjacob\",\"followers\":[1,2,3]}}");
    assertEquals("johnjacob", json.getString("user.username"));
    assertEquals(List.of(2, 3), json.getList("user.followers.findAll { it > 1 }"));

    XmlPath xml = new XmlPath("<user><username>johnjacob</username></user>");
    assertEquals("johnjacob", xml.getString("user.username"));
  }
}
