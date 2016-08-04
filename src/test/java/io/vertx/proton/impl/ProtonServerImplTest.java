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
package io.vertx.proton.impl;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import io.vertx.proton.sasl.ProtonSaslAuthenticatorFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.Sasl.SaslOutcome;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ProtonServerImplTest {

  private static final String GOOD_USER = "GOOD_USER";
  private static final String BAD_USER = "BAD_USER";
  private static final String PASSWD = "GOOD_PASSWORD";

  private Vertx vertx;

  @Before
  public void setup() {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown() {
    if (vertx != null) {
      vertx.close();
    }
  }

  @Test(timeout = 20000)
  public void testCustomAuthenticatorHasInitCalled(TestContext context) {
    Async initCalledAsync = context.async();

    ProtonServer.create(vertx).saslAuthenticatorFactory(new ProtonSaslAuthenticatorFactory() {
      @Override
      public ProtonSaslAuthenticator create() {
        return new ProtonSaslAuthenticator() {
          @Override
          public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
            initCalledAsync.complete();
          }

          @Override
          public boolean process() {
            return false;
          }

          @Override
          public boolean succeeded() {
            return false;
          }
        };
      }
    }).connectHandler(protonConnection -> {
    }).listen(server -> ProtonClient.create(vertx).connect("localhost", server.result().actualPort(),
        protonConnectionAsyncResult -> {
        }));
  }

  @Test(timeout = 20000)
  public void testCustomAuthenticatorFailsAuthentication(TestContext context) {
    Async connectedAsync = context.async();

    ProtonServer.create(vertx).saslAuthenticatorFactory(new TestAuthenticatorFactory()).connectHandler(protonConnection -> {
      context.fail("Handler should not be called for connection that failed authentication");
    }).listen(server -> ProtonClient.create(vertx).connect("localhost", server.result().actualPort(), BAD_USER, PASSWD,
        protonConnectionAsyncResult -> {
          context.assertFalse(protonConnectionAsyncResult.succeeded());
          connectedAsync.complete();
        }));

    connectedAsync.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testCustomAuthenticatorSuceedsAuthentication(TestContext context) {
    Async connectedAsync = context.async();
    Async authenticatedAsync = context.async();

    ProtonServer.create(vertx).saslAuthenticatorFactory(new TestAuthenticatorFactory()).connectHandler(protonConnection -> {
      // Verify the expected auth detail was recorded in the connection attachments, just using a String here.
      String authValue = protonConnection.attachments().get(TestAuthenticator.AUTH_KEY, String.class);
      context.assertEquals(TestAuthenticator.AUTH_VALUE, authValue);
      authenticatedAsync.complete();
    }).listen(server -> ProtonClient.create(vertx).connect("localhost", server.result().actualPort(), GOOD_USER, PASSWD,
        protonConnectionAsyncResult -> {
          context.assertTrue(protonConnectionAsyncResult.succeeded());
          protonConnectionAsyncResult.result().disconnect();
          connectedAsync.complete();
        }));

    authenticatedAsync.awaitSuccess();
    connectedAsync.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testAuthenticatorCreatedPerConnection(TestContext context) {
    Async connectedAsync = context.async();
    Async connectedAsync2 = context.async();
    AtomicInteger port = new AtomicInteger(-1);

    final TestAuthenticatorFactory authenticatorFactory = new TestAuthenticatorFactory();

    ProtonServer.create(vertx).saslAuthenticatorFactory(authenticatorFactory).connectHandler(protonConnection -> {
      // Verify the expected auth detail was recorded in the connection attachments, just using a String here.
      String authValue = protonConnection.attachments().get(TestAuthenticator.AUTH_KEY, String.class);
      context.assertEquals(TestAuthenticator.AUTH_VALUE, authValue);
    }).listen(server -> {
      port.set(server.result().actualPort());
      ProtonClient.create(vertx).connect("localhost", port.intValue(), GOOD_USER, PASSWD,
          protonConnectionAsyncResult -> {
            context.assertTrue(protonConnectionAsyncResult.succeeded());
            protonConnectionAsyncResult.result().disconnect();
            connectedAsync.complete();
          });
    });

    connectedAsync.awaitSuccess();

    context.assertEquals(1, authenticatorFactory.getCreateCount(), "unexpected authenticator count");

    ProtonClient.create(vertx).connect("localhost", port.intValue(), GOOD_USER, PASSWD, protonConnectionAsyncResult -> {
      context.assertTrue(protonConnectionAsyncResult.succeeded());
      protonConnectionAsyncResult.result().disconnect();
      connectedAsync2.complete();
    });

    connectedAsync2.awaitSuccess();

    context.assertEquals(2, authenticatorFactory.getCreateCount(), "unexpected authenticator count");
  }

  private final class TestAuthenticatorFactory implements ProtonSaslAuthenticatorFactory {
    private AtomicInteger count = new AtomicInteger(0);

    @Override
    public ProtonSaslAuthenticator create() {
      count.incrementAndGet();
      return new TestAuthenticator();
    }

    public int getCreateCount() {
      return count.intValue();
    }
  }

  private final class TestAuthenticator implements ProtonSaslAuthenticator {
    public static final String AUTH_KEY = "MY_AUTH_KEY";
    public static final String AUTH_VALUE = "MY_AUTH_VALUE";
    private static final String PLAIN = "PLAIN";

    private Sasl sasl;
    private boolean succeeded;
    ProtonConnection protonConnection;

    @Override
    public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
      this.protonConnection = protonConnection;
      this.sasl = transport.sasl();
      sasl.server();
      sasl.allowSkip(false);
      sasl.setMechanisms(PLAIN);
    }

    @Override
    public boolean process() {
      String[] remoteMechanisms = sasl.getRemoteMechanisms();
      if (remoteMechanisms.length > 0) {
        String chosenMech = remoteMechanisms[0];

        boolean success = false;
        if (PLAIN.equals(chosenMech)) {
          success = evaluatePlainResponse(sasl);
        }

        if (success) {
          succeeded = true;
          sasl.done(SaslOutcome.PN_SASL_OK);
          // Record any desired kind of auth detail in the connection attachments, just using a String here.
          protonConnection.attachments().set(AUTH_KEY, String.class, AUTH_VALUE);
        } else {
          sasl.done(SaslOutcome.PN_SASL_AUTH);
        }

        return true;
      }

      return false;
    }

    @Override
    public boolean succeeded() {
      return succeeded;
    }

    private boolean evaluatePlainResponse(Sasl sasl) {
      byte[] response = new byte[sasl.pending()];
      sasl.recv(response, 0, response.length);

      // Per https://tools.ietf.org/html/rfc4616 the PLAIN message format is: [authzid] UTF8NUL authcid UTF8NUL passwd
      // Break initial response into its constituent parts.
      int authzidTerminatorPos = findNullPosition(response, 0);
      if (authzidTerminatorPos < 0) {
        // Invalid PLAIN encoding, authzid null terminator not found
        return false;
      }

      int authcidTerminatorPos = findNullPosition(response, authzidTerminatorPos + 1);
      if (authcidTerminatorPos < 0) {
        // Invalid PLAIN encoding, authcid null terminator not found
        return false;
      }

      if (authcidTerminatorPos == response.length - 1) {
        // Invalid PLAIN encoding, no password present
        return false;
      }

      // Grab the authcid and password (ignoring authzid if present)
      String authcid = new String(response, authzidTerminatorPos + 1, authcidTerminatorPos - authzidTerminatorPos - 1,
          StandardCharsets.UTF_8);
      String passwd = new String(response, authcidTerminatorPos + 1, response.length - authcidTerminatorPos - 1,
          StandardCharsets.UTF_8);

      // Now verify the given credentials
      if (GOOD_USER.equals(authcid) && PASSWD.equals(passwd)) {
        // Success
        return true;
      }

      return false;
    }

    private int findNullPosition(byte[] response, int startPosition) {
      int position = startPosition;
      while (position < response.length) {
        if (response[position] == (byte) 0) {
          return position;
        }
        position++;
      }
      return -1;
    }
  }

}
