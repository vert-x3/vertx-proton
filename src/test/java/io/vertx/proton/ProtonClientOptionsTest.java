package io.vertx.proton;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProtonClientOptionsTest {

  @Test
  public void testEqualsItself() {
    ProtonClientOptions options = new ProtonClientOptions();

    assertEquals("Options should be equal to itself", options, options);
  }

  @Test
  public void testDifferentObjectsEqual() {
    ProtonClientOptions options1 = new ProtonClientOptions();
    options1.setAllowedSaslMechanisms("PLAIN");
    ProtonClientOptions options2 = new ProtonClientOptions();
    options2.setAllowedSaslMechanisms("PLAIN");

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
  }

  @Test
  public void testDifferentObjectsNotEqual() {
    ProtonClientOptions options1 = new ProtonClientOptions();
    options1.setAllowedSaslMechanisms("PLAIN");
    ProtonClientOptions options2 = new ProtonClientOptions();
    options2.setAllowedSaslMechanisms("ANONYMOUS");

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }

  @Test
  public void testEqualObjectsReturnSameHashCode() {
    ProtonClientOptions options1 = new ProtonClientOptions();
    options1.setAllowedSaslMechanisms("PLAIN");
    ProtonClientOptions options2 = new ProtonClientOptions();
    options2.setAllowedSaslMechanisms("PLAIN");

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
    assertEquals("Options should have same hash code", options1.hashCode(), options2.hashCode());
  }

  @Test
  public void testHashCodeReturnsSameValueOnRepeatedCall() {
    ProtonClientOptions options = new ProtonClientOptions();
    options.setAllowedSaslMechanisms("PLAIN");

    assertEquals("Options should have same hash code for both calls", options.hashCode(), options.hashCode());
  }

}
