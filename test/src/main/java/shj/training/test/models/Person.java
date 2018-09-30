package shj.training.test.models;

import org.henj.annotations.BuilderProperty;

/**
 * @Project henj
 * @Author Henri Joel SEDJAME
 * @Date 30/09/2018
 * @Class purposes : .......
 */

public class Person {

  private String name;
  private int age;

  @BuilderProperty
  public void setName(String name) {
    this.name = name;
  }

  @BuilderProperty
  public void setAge(int age) {
    this.age = age;
  }

  public String getName() {
    return name;
  }

  public int getAge() {
    return age;
  }
}
