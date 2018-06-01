/*
* Copyright 2018 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.vertx.proton.streams;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProtonPublisherOptionsTest {

  @Test
  public void testEqualsItself() {
    ProtonPublisherOptions options = new ProtonPublisherOptions();

    assertEquals("Options should be equal to itself", options, options);
  }

  @Test
  public void testEqualsNull() {
    ProtonPublisherOptions options = new ProtonPublisherOptions();

    assertFalse("Options should not equal null", options.equals(null));
  }

  @Test
  public void testDifferentObjectsEqual() {
    ProtonPublisherOptions options1 = new ProtonPublisherOptions();
    options1.setLinkName("SAME");
    options1.setDurable(true);
    options1.setShared(true);
    options1.setGlobal(true);
    options1.setDynamic(true);
    options1.setMaxOutstandingCredit(1234);
    ProtonPublisherOptions options2 = new ProtonPublisherOptions();
    options2.setLinkName("SAME");
    options2.setDurable(true);
    options2.setShared(true);
    options2.setGlobal(true);
    options2.setDynamic(true);
    options2.setMaxOutstandingCredit(1234);

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
  }

  @Test
  public void testEqualObjectsReturnSameHashCode() {
    ProtonPublisherOptions options1 = new ProtonPublisherOptions();
    options1.setLinkName("SAME");
    options1.setDurable(true);
    options1.setShared(true);
    options1.setGlobal(true);
    options1.setDynamic(true);
    options1.setMaxOutstandingCredit(1234);
    ProtonPublisherOptions options2 = new ProtonPublisherOptions();
    options2.setLinkName("SAME");
    options2.setDurable(true);
    options2.setShared(true);
    options2.setGlobal(true);
    options2.setDynamic(true);
    options2.setMaxOutstandingCredit(1234);

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
    assertEquals("Options should have same hash code", options1.hashCode(), options2.hashCode());
  }

  @Test
  public void testHashCodeReturnsSameValueOnRepeatedCall() {
    ProtonPublisherOptions options = new ProtonPublisherOptions();
    options.setLinkName("name");

    assertEquals("Options should have same hash code for both calls", options.hashCode(), options.hashCode());
  }

  @Test
  public void testLinkName() {
    ProtonPublisherOptions options1 = new ProtonPublisherOptions();
    assertNull("Expected no default name", options1.getLinkName());
    options1.setLinkName("name");
    assertEquals("name", options1.getLinkName());

    ProtonPublisherOptions options2 = new ProtonPublisherOptions();
    options2.setLinkName("other-name");
    assertEquals("other-name", options2.getLinkName());

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }

  @Test
  public void testDurable() {
    ProtonPublisherOptions options1 = new ProtonPublisherOptions();
    assertFalse("Expected durable to be false by default", options1.isDurable());
    options1.setDurable(true);
    assertTrue("Expected durable to be true", options1.isDurable());

    ProtonPublisherOptions options2 = new ProtonPublisherOptions();
    assertFalse("Expected durable to be false by default", options2.isDurable());

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }

  @Test
  public void testShared() {
    ProtonPublisherOptions options1 = new ProtonPublisherOptions();
    assertFalse("Expected shared to be false by default", options1.isShared());
    options1.setShared(true);
    assertTrue("Expected shared to be true", options1.isShared());

    ProtonPublisherOptions options2 = new ProtonPublisherOptions();
    assertFalse("Expected shared to be false by default", options2.isShared());

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }

  @Test
  public void testGlobal() {
    ProtonPublisherOptions options1 = new ProtonPublisherOptions();
    assertFalse("Expected global to be false by default", options1.isGlobal());
    options1.setGlobal(true);
    assertTrue("Expected global to be true", options1.isGlobal());

    ProtonPublisherOptions options2 = new ProtonPublisherOptions();
    assertFalse("Expected global to be false by default", options2.isGlobal());

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }

  @Test
  public void testDynamic() {
    ProtonPublisherOptions options1 = new ProtonPublisherOptions();
    assertFalse("Expected dynamic to be false by default", options1.isDynamic());
    options1.setDynamic(true);
    assertTrue("Expected dynamic to be true", options1.isDynamic());

    ProtonPublisherOptions options2 = new ProtonPublisherOptions();
    assertFalse("Expected dynamic to be false by default", options2.isDynamic());

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }

  @Test
  public void testMaxOutstandingCredit() {
    ProtonPublisherOptions options1 = new ProtonPublisherOptions();
    assertEquals("Expected maxOutstandingCredit to be 0 default", 0, options1.getMaxOutstandingCredit());
    options1.setMaxOutstandingCredit(12345);
    assertEquals("Unexpected maxOutstandingCredit", 12345, options1.getMaxOutstandingCredit());

    ProtonPublisherOptions options2 = new ProtonPublisherOptions();
    assertEquals("Expected maxOutstandingCredit to be 0 default", 0, options2.getMaxOutstandingCredit());

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }
}
