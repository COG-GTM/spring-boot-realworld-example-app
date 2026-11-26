package io.spring;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaVersionTest {

  private static final int JAVA_17_CLASS_FILE_MAJOR_VERSION = 61;

  @Test
  public void should_compile_production_classes_for_java_17() throws IOException {
    Assertions.assertEquals(
        JAVA_17_CLASS_FILE_MAJOR_VERSION, classFileMajorVersion(RealWorldApplication.class));
  }

  @Test
  public void should_run_tests_on_java_17_or_newer() {
    Assertions.assertTrue(Runtime.version().feature() >= 17);
  }

  private static int classFileMajorVersion(Class<?> clazz) throws IOException {
    String resource = "/" + clazz.getName().replace('.', '/') + ".class";
    try (InputStream in = clazz.getResourceAsStream(resource);
        DataInputStream data = new DataInputStream(in)) {
      Assertions.assertEquals(0xCAFEBABE, data.readInt());
      data.readUnsignedShort();
      return data.readUnsignedShort();
    }
  }
}
