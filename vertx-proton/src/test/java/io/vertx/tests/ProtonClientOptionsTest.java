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
package io.vertx.tests;

import static org.junit.Assert.*;

import io.vertx.proton.ProtonClientOptions;
import org.junit.Test;

public class ProtonClientOptionsTest {

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

  @Test
  public void testSetUseAlpnNoops() {
    ProtonClientOptions options = new ProtonClientOptions();
    assertFalse("Alpn should be disabled by default", options.isUseAlpn());

    // Check setting it false does nothing
    options.setUseAlpn(false);
    assertFalse("Alpn should still be disabled", options.isUseAlpn());

    // Check setting it true is also a noop (logs a message saying so)
    options.setUseAlpn(true);
    assertFalse("Alpn should still be disabled", options.isUseAlpn());
  }
}
