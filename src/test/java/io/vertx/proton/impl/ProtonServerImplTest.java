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

import io.vertx.core.Context;
import io.vertx.core.Handler;
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
import java.util.concurrent.atomic.AtomicBoolean;
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

  private static final String PLAIN = "PLAIN";
  private static final String AUTH_KEY = "MY_AUTH_KEY";
  private static final String AUTH_VALUE = "MY_AUTH_VALUE";

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
          public void process(Handler<Boolean> completionHandler) {
            completionHandler.handle(false);
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

    ProtonServer.create(vertx).saslAuthenticatorFactory(new TestPlainAuthenticatorFactory()).connectHandler(protonConnection -> {
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

    ProtonServer.create(vertx).saslAuthenticatorFactory(new TestPlainAuthenticatorFactory()).connectHandler(protonConnection -> {
      // Verify the expected auth detail was recorded in the connection attachments, just using a String here.
      String authValue = protonConnection.attachments().get(AUTH_KEY, String.class);
      context.assertEquals(AUTH_VALUE, authValue);
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

    final TestPlainAuthenticatorFactory authenticatorFactory = new TestPlainAuthenticatorFactory();

    ProtonServer.create(vertx).saslAuthenticatorFactory(authenticatorFactory).connectHandler(protonConnection -> {
      // Verify the expected auth detail was recorded in the connection attachments, just using a String here.
      String authValue = protonConnection.attachments().get(AUTH_KEY, String.class);
      context.assertEquals(AUTH_VALUE, authValue);
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

  private final class TestPlainAuthenticatorFactory implements ProtonSaslAuthenticatorFactory {
    private AtomicInteger count = new AtomicInteger(0);

    @Override
    public ProtonSaslAuthenticator create() {
      count.incrementAndGet();
      return new TestPlainAuthenticator();
    }

    public int getCreateCount() {
      return count.intValue();
    }
  }

  private final class TestPlainAuthenticator implements ProtonSaslAuthenticator {
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
    public void process(Handler<Boolean> processComplete) {
      boolean done = false;
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

        done = true;
      }

      processComplete.handle(done);
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

  @Test(timeout = 20000)
  public void testAsyncAuthenticatorSucceed(TestContext context) {
    doTestAsyncServerAuthenticatorTestImpl(context, true);
  }

  @Test(timeout = 20000)
  public void testAsyncAuthenticatorFail(TestContext context) {
    doTestAsyncServerAuthenticatorTestImpl(context, false);
  }

  private void doTestAsyncServerAuthenticatorTestImpl(TestContext context, boolean passAuthentication) {
    Async connectAsync = context.async();
    AtomicBoolean connectedServer = new AtomicBoolean();

    final long delay = 750;
    TestAsyncAuthenticator testAsyncAuthenticator = new TestAsyncAuthenticator(delay, passAuthentication);
    TestAsyncAuthenticatorFactory authenticatorFactory = new TestAsyncAuthenticatorFactory(testAsyncAuthenticator);

    ProtonServer.create(vertx).saslAuthenticatorFactory(authenticatorFactory).connectHandler(protonConnection -> {
      connectedServer.set(true);
    }).listen(server -> {
      final long startTime = System.currentTimeMillis();
      ProtonClient.create(vertx).connect("localhost", server.result().actualPort(), GOOD_USER, PASSWD, conResult -> {
        // Verify the process took expected time from auth delay.
        long actual = System.currentTimeMillis() - startTime;
        context.assertTrue(actual >= delay, "Connect completed before expected time delay elapsed! " + actual);

        if (passAuthentication) {
          context.assertTrue(conResult.succeeded(), "Expected connect to succeed");
          conResult.result().disconnect();
        } else {
          context.assertFalse(conResult.succeeded(), "Expected connect to fail");
        }

        connectAsync.complete();
      });
    });

    connectAsync.awaitSuccess();

    if(passAuthentication) {
      context.assertTrue(connectedServer.get(), "Server handler should have been called");
    } else {
      context.assertFalse(connectedServer.get(), "Server handler should not have been called");
    }

    context.assertEquals(1, authenticatorFactory.getCreateCount(), "unexpected authenticator creation count");
  }

  private final class TestAsyncAuthenticatorFactory implements ProtonSaslAuthenticatorFactory {
    private ProtonSaslAuthenticator authenticator;
    private AtomicInteger count = new AtomicInteger(0);

    public TestAsyncAuthenticatorFactory(TestAsyncAuthenticator authenticator) {
      this.authenticator = authenticator;
    }

    @Override
    public ProtonSaslAuthenticator create() {
      count.incrementAndGet();
      return authenticator;
    }

    public int getCreateCount() {
      return count.intValue();
    }
  }

  private final class TestAsyncAuthenticator implements ProtonSaslAuthenticator {
    private final long completionDelay;
    private final boolean passAuth;

    private Sasl sasl;
    private boolean succeeded;

    public TestAsyncAuthenticator(long completionDelay, boolean passAuth) {
      this.completionDelay = completionDelay;
      this.passAuth = passAuth;
    }

    @Override
    public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
      this.sasl = transport.sasl();
      sasl.server();
      sasl.allowSkip(false);
      sasl.setMechanisms(PLAIN);
    }

    @Override
    public void process(Handler<Boolean> processComplete) {
      String[] remoteMechanisms = sasl.getRemoteMechanisms();
      if (remoteMechanisms.length > 0) {
        String chosenMech = remoteMechanisms[0];

        if (PLAIN.equals(chosenMech)) {
          Context context = Vertx.currentContext();

          byte[] response = new byte[sasl.pending()];
          sasl.recv(response, 0, response.length);

          // Signal process handling completed (with success/failure also in this case) only after the given delay.
          // The timer scheduling will use the same Context for the callback automatically in this case.
          context.owner().setTimer(completionDelay, x -> {
            if (passAuth) {
              succeeded = true;
              sasl.done(SaslOutcome.PN_SASL_OK);
            } else {
              sasl.done(SaslOutcome.PN_SASL_AUTH);
            }

            processComplete.handle(true);
          });
        } else {
          sasl.done(SaslOutcome.PN_SASL_AUTH);
          processComplete.handle(true);
        }
      } else {
        processComplete.handle(false);
      }
    }

    @Override
    public boolean succeeded() {
      return succeeded;
    }
  }
}
