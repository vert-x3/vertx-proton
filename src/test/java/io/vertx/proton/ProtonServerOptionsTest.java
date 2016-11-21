/*
* Copyright 2016 the original author or authors.
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
package io.vertx.proton;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProtonServerOptionsTest {

  @Test
  public void testEqualsItself() {
    ProtonServerOptions options = new ProtonServerOptions();

    assertEquals("Options should be equal to itself", options, options);
  }

  @Test
  public void testDifferentObjectsEqual() {
    ProtonServerOptions options1 = new ProtonServerOptions();
    options1.setHeartbeat(1000);
    ProtonServerOptions options2 = new ProtonServerOptions();
    options2.setHeartbeat(1000);

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
  }

  @Test
  public void testDifferentObjectsNotEqual() {
    ProtonServerOptions options1 = new ProtonServerOptions();
    options1.setHeartbeat(1000);
    ProtonServerOptions options2 = new ProtonServerOptions();
    options2.setHeartbeat(2000);

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }

  @Test
  public void testEqualObjectsReturnSameHashCode() {
    ProtonServerOptions options1 = new ProtonServerOptions();
    options1.setHeartbeat(1000);
    ProtonServerOptions options2 = new ProtonServerOptions();
    options2.setHeartbeat(1000);

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
    assertEquals("Options should have same hash code", options1.hashCode(), options2.hashCode());
  }

  @Test
  public void testEqualsNull() {
    ProtonServerOptions options = new ProtonServerOptions();

    assertFalse("Options should not equal null", options.equals(null));
  }

  @Test
  public void testHashCodeReturnsSameValueOnRepeatedCall() {
    ProtonServerOptions options = new ProtonServerOptions();
    options.setHeartbeat(1000);

    assertEquals("Options should have same hash code for both calls", options.hashCode(), options.hashCode());
  }

  @Test
  public void testHeartbeat() {
    ProtonServerOptions options = new ProtonServerOptions();

    options.setHeartbeat(1000);
    assertEquals(options.getHeartbeat(), 1000);
    options.setHeartbeat(2000);
    assertNotEquals(options.getHeartbeat(), 1000);
  }
}
