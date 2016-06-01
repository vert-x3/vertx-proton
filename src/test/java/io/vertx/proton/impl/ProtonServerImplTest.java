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
import io.vertx.proton.sasl.impl.ProtonSaslAnonymousImpl;

import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.Sasl.SaslOutcome;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ProtonServerImplTest {

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

    ProtonServer.create(vertx).saslAuthenticator(new ProtonSaslAuthenticator() {
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
    }).connectHandler(protonConnection -> {
    }).listen(server -> ProtonClient.create(vertx).connect("localhost", server.result().actualPort(),
        protonConnectionAsyncResult -> {
        }));
  }

  @Test(timeout = 20000)
  public void testCustomAuthenticatorFailsAuthentication(TestContext context) {
    Async connectedAsync = context.async();

    ProtonServer.create(vertx).saslAuthenticator(new TestAuthenticator(false)).connectHandler(protonConnection -> {
      context.fail("Handler should not be called for connection that failed authentication");
    }).listen(server -> ProtonClient.create(vertx).connect("localhost", server.result().actualPort(),
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

    ProtonServer.create(vertx).saslAuthenticator(new TestAuthenticator(true)).connectHandler(protonConnection -> {
      // Verify the expected auth detail was recorded in the connection attachments, just using a String here.
      String authValue = protonConnection.attachments().get(TestAuthenticator.AUTH_KEY, String.class);
      context.assertEquals(TestAuthenticator.AUTH_VALUE, authValue);
      authenticatedAsync.complete();
    }).listen(server -> ProtonClient.create(vertx).connect("localhost", server.result().actualPort(),
        protonConnectionAsyncResult -> {
          context.assertTrue(protonConnectionAsyncResult.succeeded());
          protonConnectionAsyncResult.result().disconnect();
          connectedAsync.complete();
        }));

    authenticatedAsync.awaitSuccess();
    connectedAsync.awaitSuccess();
  }

  private final class TestAuthenticator implements ProtonSaslAuthenticator {
    public static final String AUTH_KEY = "MY_AUTH_KEY";
    public static final String AUTH_VALUE = "MY_AUTH_VALUE";

    private Sasl sasl;
    private boolean succeed;
    private boolean succeeded;
    ProtonConnection protonConnection;

    public TestAuthenticator(boolean succeed) {
      this.succeed = succeed;
    }

    @Override
    public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
      this.protonConnection = protonConnection;
      this.sasl = transport.sasl();
      sasl.server();
      sasl.allowSkip(false);
      sasl.setMechanisms(ProtonSaslAnonymousImpl.MECH_NAME);
    }

    @Override
    public boolean process() {
      String[] remoteMechanisms = sasl.getRemoteMechanisms();
      if (remoteMechanisms.length > 0) {
        if (succeed) {
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
  }

}
