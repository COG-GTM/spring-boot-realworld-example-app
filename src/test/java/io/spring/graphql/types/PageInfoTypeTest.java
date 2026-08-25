package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PageInfoTypeTest {
  @Test
  public void should_build_page_info_with_builder() {
    PageInfo pageInfo =
        PageInfo.newBuilder()
            .startCursor("start")
            .endCursor("end")
            .hasNextPage(true)
            .hasPreviousPage(false)
            .build();

    assertThat(pageInfo.getStartCursor()).isEqualTo("start");
    assertThat(pageInfo.getEndCursor()).isEqualTo("end");
    assertThat(pageInfo.getHasNextPage()).isTrue();
    assertThat(pageInfo.getHasPreviousPage()).isFalse();
  }

  @Test
  public void should_construct_page_info_with_all_args_constructor() {
    PageInfo pageInfo = new PageInfo("end", true, true, "start");

    assertThat(pageInfo.getEndCursor()).isEqualTo("end");
    assertThat(pageInfo.getHasNextPage()).isTrue();
    assertThat(pageInfo.getHasPreviousPage()).isTrue();
    assertThat(pageInfo.getStartCursor()).isEqualTo("start");
  }

  @Test
  public void should_set_fields_with_setters() {
    PageInfo pageInfo = new PageInfo();
    assertThat(pageInfo.getEndCursor()).isNull();

    pageInfo.setEndCursor("e");
    pageInfo.setStartCursor("s");
    pageInfo.setHasNextPage(true);
    pageInfo.setHasPreviousPage(true);

    assertThat(pageInfo.getEndCursor()).isEqualTo("e");
    assertThat(pageInfo.getStartCursor()).isEqualTo("s");
    assertThat(pageInfo.getHasNextPage()).isTrue();
    assertThat(pageInfo.getHasPreviousPage()).isTrue();
  }

  @Test
  public void should_render_all_fields_in_to_string() {
    PageInfo pageInfo = new PageInfo("end", true, false, "start");

    assertThat(pageInfo.toString())
        .startsWith("PageInfo{")
        .contains("endCursor='end'")
        .contains("startCursor='start'")
        .contains("hasNextPage='true'")
        .contains("hasPreviousPage='false'");
  }

  @Test
  public void should_compare_by_value() {
    PageInfo one = new PageInfo("end", true, false, "start");
    PageInfo same = new PageInfo("end", true, false, "start");
    PageInfo other = new PageInfo("end", false, false, "start");

    assertThat(one).isEqualTo(one).isEqualTo(same).isNotEqualTo(other).isNotEqualTo(null);
    assertThat(one.equals("not a page info")).isFalse();
    assertThat(one).hasSameHashCodeAs(same);
    assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
  }
}
