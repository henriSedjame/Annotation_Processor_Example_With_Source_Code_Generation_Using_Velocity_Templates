package shj.training.test.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Project henj
 * @Author Henri Joel SEDJAME
 * @Date 30/09/2018
 * @Class purposes : .......
 */
class PersonTest {

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
  }

  @Test
  void testBuilder() {
    Person person = new PersonBuilder()
                .withName("Henri")
                .withAge(31)
                .build();
    assertAll("person",
      () -> assertEquals("Henri", person.getName()),
      ()-> assertEquals(31, person.getAge())
      );
  }
}
