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
package io.vertx.tests.streams;

import static org.junit.Assert.*;

import io.vertx.proton.streams.ProtonSubscriberOptions;
import org.junit.Test;

public class ProtonSubscriberOptionsTest {

  @Test
  public void testEqualsItself() {
    ProtonSubscriberOptions options = new ProtonSubscriberOptions();

    assertEquals("Options should be equal to itself", options, options);
  }

  @Test
  public void testEqualsNull() {
    ProtonSubscriberOptions options = new ProtonSubscriberOptions();

    assertFalse("Options should not equal null", options.equals(null));
  }

  @Test
  public void testDifferentObjectsEqual() {
    ProtonSubscriberOptions options1 = new ProtonSubscriberOptions();
    options1.setLinkName("SAME");
    ProtonSubscriberOptions options2 = new ProtonSubscriberOptions();
    options2.setLinkName("SAME");

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
  }

  @Test
  public void testEqualObjectsReturnSameHashCode() {
    ProtonSubscriberOptions options1 = new ProtonSubscriberOptions();
    options1.setLinkName("SAME");
    ProtonSubscriberOptions options2 = new ProtonSubscriberOptions();
    options2.setLinkName("SAME");

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
    assertEquals("Options should have same hash code", options1.hashCode(), options2.hashCode());
  }

  @Test
  public void testHashCodeReturnsSameValueOnRepeatedCall() {
    ProtonSubscriberOptions options = new ProtonSubscriberOptions();
    options.setLinkName("name");

    assertEquals("Options should have same hash code for both calls", options.hashCode(), options.hashCode());
  }

  @Test
  public void testLinkName() {
    ProtonSubscriberOptions options1 = new ProtonSubscriberOptions();
    assertNull("Expected no default name", options1.getLinkName());
    options1.setLinkName("name");
    assertEquals("name", options1.getLinkName());

    ProtonSubscriberOptions options2 = new ProtonSubscriberOptions();
    options2.setLinkName("other-name");
    assertEquals("other-name", options2.getLinkName());

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }
}
