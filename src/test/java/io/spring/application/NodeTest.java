package io.spring.application;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class NodeTest {

  private static class TestNode implements Node {
    private final DateTime time;

    TestNode(DateTime time) {
      this.time = time;
    }

    @Override
    public PageCursor getCursor() {
      return new DateTimeCursor(time);
    }
  }

  @Test
  public void should_return_cursor_backed_by_node_data() {
    DateTime time = new DateTime();
    Node node = new TestNode(time);
    PageCursor cursor = node.getCursor();
    assertThat(cursor.getData(), is(time));
    assertThat(cursor.toString(), is(String.valueOf(time.getMillis())));
  }
}
