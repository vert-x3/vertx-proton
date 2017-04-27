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
    options1.addEnabledSaslMechanism("PLAIN");
    ProtonClientOptions options2 = new ProtonClientOptions();
    options2.addEnabledSaslMechanism("PLAIN");

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
  }

  @Test
  public void testDifferentObjectsNotEqual() {
    ProtonClientOptions options1 = new ProtonClientOptions();
    options1.addEnabledSaslMechanism("PLAIN");
    ProtonClientOptions options2 = new ProtonClientOptions();
    options2.addEnabledSaslMechanism("ANONYMOUS");

    assertNotSame("Options should be different objects", options1, options2);
    assertNotEquals("Options should not be equal", options1, options2);
  }

  @Test
  public void testEqualObjectsReturnSameHashCode() {
    ProtonClientOptions options1 = new ProtonClientOptions();
    options1.addEnabledSaslMechanism("PLAIN");
    ProtonClientOptions options2 = new ProtonClientOptions();
    options2.addEnabledSaslMechanism("PLAIN");

    assertNotSame("Options should be different objects", options1, options2);
    assertEquals("Options should be equal", options1, options2);
    assertEquals("Options should have same hash code", options1.hashCode(), options2.hashCode());
  }

  @Test
  public void testEqualsNull() {
    ProtonClientOptions options = new ProtonClientOptions();

    assertFalse("Options should not equal null", options.equals(null));
  }

  @Test
  public void testAddGetEnabledSaslMechanisms() {
    ProtonClientOptions options = new ProtonClientOptions();

    assertTrue("Enabled mechanisms should be empty", options.getEnabledSaslMechanisms().isEmpty());

    options.addEnabledSaslMechanism("PLAIN");

    assertFalse("Enabled mechanisms should not be empty", options.getEnabledSaslMechanisms().isEmpty());
    assertTrue("Enabled mechanisms should contain PLAIN", options.getEnabledSaslMechanisms().contains("PLAIN"));
  }

  @Test
  public void testHashCodeReturnsSameValueOnRepeatedCall() {
    ProtonClientOptions options = new ProtonClientOptions();
    options.addEnabledSaslMechanism("PLAIN");

    assertEquals("Options should have same hash code for both calls", options.hashCode(), options.hashCode());
  }

  @Test
  public void testDefaultHostnameVerificationAlgorithm() {
    ProtonClientOptions options = new ProtonClientOptions();

    assertEquals("Expected default verification algorithm to be HTTPS", "HTTPS",
        options.getHostnameVerificationAlgorithm());
  }

  @Test
  public void testSetGetHostnameVerificationAlgorithm() {
    ProtonClientOptions options = new ProtonClientOptions();

    options.setHostnameVerificationAlgorithm("");
    assertEquals("Expected algorthim value not found", "", options.getHostnameVerificationAlgorithm());
    options.setHostnameVerificationAlgorithm("HTTPS");
    assertEquals("Expected algorthim value not found", "HTTPS", options.getHostnameVerificationAlgorithm());
  }

  @Test
  public void testHeartbeat() {
    ProtonClientOptions options = new ProtonClientOptions();

    options.setHeartbeat(1000);
    assertEquals(options.getHeartbeat(), 1000);
    options.setHeartbeat(2000);
    assertNotEquals(options.getHeartbeat(), 1000);
  }

  @Test
  public void testVirtualHost() {
    ProtonClientOptions options = new ProtonClientOptions();
    options.setVirtualHost("example.com");
    assertEquals("example.com", options.getVirtualHost());
    options.setVirtualHost("another.example.com");
    assertEquals("another.example.com", options.getVirtualHost());
  }

  @Test
  public void testSniServerName() {
    ProtonClientOptions options = new ProtonClientOptions();
    options.setSniServerName("example.com");
    assertEquals("example.com", options.getSniServerName());
    options.setSniServerName("another.example.com");
    assertEquals("another.example.com", options.getSniServerName());
  }
}
