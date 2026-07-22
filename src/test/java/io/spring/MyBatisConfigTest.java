package io.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

public class MyBatisConfigTest {

  @Test
  public void should_be_instantiable() {
    assertThat(new MyBatisConfig(), is(notNullValue()));
  }

  @Test
  public void should_be_annotated_as_configuration() {
    assertThat(MyBatisConfig.class.isAnnotationPresent(Configuration.class), is(true));
  }

  @Test
  public void should_enable_transaction_management() {
    assertThat(
        MyBatisConfig.class.isAnnotationPresent(EnableTransactionManagement.class), is(true));
  }
}
