package io.spring;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.bytebuddy.ClassFileVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

/**
 * Guards the test stack itself against JDK strong encapsulation. Mockito mocks through byte-buddy
 * and rest-assured's JsonPath/XmlPath go through Groovy; both reach into areas the JDK
 * progressively locks down, so a JDK bump can break them while production code stays fine.
 */
class TestStackJdk17Test {

  @Test
  void byteBuddyShouldSupportTheRunningJvmWithoutExperimentalMode() {
    // ofThisVm() throws rather than returning a sentinel when byte-buddy cannot map java.version,
    // so the throwing case is folded in here to keep the "bump byte-buddy" hint on both paths.
    ClassFileVersion running =
        assertDoesNotThrow(
            (ThrowingSupplier<ClassFileVersion>) ClassFileVersion::ofThisVm,
            "byte-buddy does not recognise this JVM; bump net.bytebuddy:byte-buddy");
    assertTrue(
        running.isAtLeast(ClassFileVersion.JAVA_V17),
        "expected to run on JDK 17+, but byte-buddy sees " + running);
    // Only trips when -Dnet.bytebuddy.experimental=true lets ofThisVm() report a version byte-buddy
    // does not natively support; without the flag that case throws and is caught above.
    assertTrue(
        running.isAtMost(ClassFileVersion.latest()),
        "byte-buddy supports this JVM only in experimental mode; bump net.bytebuddy:byte-buddy");
  }

  @Test
  void mockitoShouldMockApplicationTypesCompiledForJava17() throws IOException {
    ClassFileVersion compiled = ClassFileVersion.of(User.class);
    assertTrue(
        compiled.isAtLeast(ClassFileVersion.JAVA_V17),
        "expected application classes at Java 17+ bytecode, but found " + compiled);

    User user = new User("john@jacob.com", "johnjacob", "123", "", "");

    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findByUsername(eq("johnjacob"))).thenReturn(Optional.of(user));
    assertEquals(Optional.of(user), userRepository.findByUsername("johnjacob"));
    verify(userRepository).findByUsername("johnjacob");

    // Subclassing a concrete class is the encapsulation-sensitive path: byte-buddy has to define
    // the
    // proxy against the target's own package rather than just implement an interface.
    ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
    when(articleQueryService.findById(eq("article-id"), eq(user))).thenReturn(Optional.empty());
    assertEquals(Optional.<ArticleData>empty(), articleQueryService.findById("article-id", user));
    verify(articleQueryService).findById("article-id", user);
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
