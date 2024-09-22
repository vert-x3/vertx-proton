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

import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLSession;
import javax.security.sasl.AuthenticationException;

import io.vertx.core.Promise;
import io.vertx.proton.*;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.Sasl.SaslOutcome;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.sasl.MechanismMismatchException;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import io.vertx.proton.sasl.impl.ProtonSaslAnonymousImpl;

@RunWith(VertxUnitRunner.class)
public class ProtonClientSaslTest extends ActiveMQTestBase {

  private static Logger LOG = LoggerFactory.getLogger(ProtonClientSaslTest.class);

  private static final String SERVER_KEYSTORE = "src/test/resources/broker-pkcs12.keystore";
  private static final String TRUSTSTORE = "src/test/resources/client-pkcs12.truststore";
  private static final String KEYSTORE_CLIENT = "src/test/resources/client-pkcs12.keystore";
  private static final String STORE_PASSWORD = "password";

  private Vertx vertx;
  private ProtonServer protonServer;

  private boolean anonymousAccessAllowed = false;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    vertx = Vertx.vertx();
  }

  @After
  @Override
  public void tearDown() throws Exception {
    try {
      super.tearDown();
    } finally {
      try {
        if (protonServer != null) {
          protonServer.close();
        }
      } finally {
        if (vertx != null) {
          vertx.close();
        }
      }
    }
  }

  @Override
  protected boolean isAnonymousAccessAllowed() {
    return anonymousAccessAllowed;
  }

  @Test(timeout = 20000)
  public void testConnectWithValidUserPassSucceeds(TestContext context) throws Exception {
    doConnectWithGivenCredentialsTestImpl(context, USERNAME_GUEST, PASSWORD_GUEST, null);
  }

  @Test(timeout = 20000)
  public void testConnectWithInvalidUserPassFails(TestContext context) throws Exception {
    doConnectWithGivenCredentialsTestImpl(context, USERNAME_GUEST, "wrongpassword", AuthenticationException.class);
  }

  @Test(timeout = 20000)
  public void testConnectAnonymousWithoutUserPass(TestContext context) throws Exception {
    doConnectWithGivenCredentialsTestImpl(context, null, null, AuthenticationException.class);
    anonymousAccessAllowed = true;
    restartBroker();
    doConnectWithGivenCredentialsTestImpl(context, null, null, null);
  }

  @Test(timeout = 20000)
  public void testRestrictSaslMechanisms(TestContext context) throws Exception {
    ProtonClientOptions options = new ProtonClientOptions();

    // Try with the wrong password, with anonymous access disabled, expect connect to fail
    doConnectWithGivenCredentialsTestImpl(context, options, USERNAME_GUEST, "wrongpassword", AuthenticationException.class);

    // Try with the wrong password, with anonymous access enabled, expect connect still to fail
    anonymousAccessAllowed = true;
    restartBroker();
    doConnectWithGivenCredentialsTestImpl(context, options, USERNAME_GUEST, "wrongpassword", AuthenticationException.class);

    // Now restrict the allows SASL mechanisms to ANONYMOUS, then expect connect to succeed as it wont use the invalid
    // credentials
    options.addEnabledSaslMechanism(ProtonSaslAnonymousImpl.MECH_NAME);
    doConnectWithGivenCredentialsTestImpl(context, options, USERNAME_GUEST, "wrongpassword", null);
  }

  @Test(timeout = 20000)
  public void testConnectWithUnsupportedSaslMechanisms(TestContext context) throws Exception {
    ProtonClientOptions options = new ProtonClientOptions();
    options.addEnabledSaslMechanism("NON_EXISTING");
    doConnectWithGivenCredentialsTestImpl(context, options, USERNAME_GUEST, "wrongpassword", MechanismMismatchException.class);
  }

  private void doConnectWithGivenCredentialsTestImpl(TestContext context, String username, String password,
                                                     Class<?> expectedException) {
    doConnectWithGivenCredentialsTestImpl(context, new ProtonClientOptions(), username, password,
        expectedException);
  }

  private void doConnectWithGivenCredentialsTestImpl(TestContext context, ProtonClientOptions options, String username,
                                                     String password, Class<?> expectedException) {
    Async async = context.async();

    // Connect the client and open the connection to verify it works
    ProtonClient client = ProtonClient.create(vertx);
    client.connect(options, "localhost", getBrokerAmqpConnectorPort(), username, password, res -> {
      if (expectedException == null) {
        // Expect connect to succeed
        context.assertTrue(res.succeeded());
        ProtonConnection connection = res.result();

        connection.openHandler(connRes -> {
          context.assertTrue(connRes.succeeded());
          LOG.trace("Client connection open");
          async.complete();
        }).open();
      } else {
        // Expect connect to fail
        context.assertFalse(res.succeeded());
        context.assertTrue(expectedException.isInstance(res.cause()));
        LOG.trace("Connect failed: " + res.cause().getMessage());
        async.complete();
      }
    });

    async.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testConnectWithSslWithClientCertSelectsOfferedExternalSaslMech(TestContext context) throws Exception {
    doExternalMechTestImpl(context, true);
  }

  @Test(timeout = 20000)
  public void testConnectWithSslWithoutClientCertIgnoresOfferedExternalSaslMech(TestContext context) throws Exception {
    doExternalMechTestImpl(context, false);
  }

  private void doExternalMechTestImpl(TestContext context, boolean supplyClientCert) throws Exception {
    stopBroker();
    Async async = context.async();

    // Create a server that accept a connection and expects a client connection
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(true);
    serverOptions.setClientAuth(supplyClientCert ? ClientAuth.REQUIRED : ClientAuth.NONE);
    PfxOptions serverPfxOptions = new PfxOptions().setPath(SERVER_KEYSTORE).setPassword(STORE_PASSWORD);
    serverOptions.setKeyCertOptions(serverPfxOptions);
    PfxOptions pfxOptions = new PfxOptions().setPath(TRUSTSTORE).setPassword(STORE_PASSWORD);
    serverOptions.setTrustOptions(pfxOptions);

    TestExternalAuthenticator authenticator = new TestExternalAuthenticator("EXTERNAL", "PLAIN", "ANONYMOUS");

    protonServer = createTestServer(serverOptions, authenticator);

    // Try to connect the client
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);
    clientOptions.setTrustOptions(pfxOptions);

    if (supplyClientCert) {
      PfxOptions clientKeyPfxOptions = new PfxOptions().setPath(KEYSTORE_CLIENT).setPassword(STORE_PASSWORD);
      clientOptions.setKeyCertOptions(clientKeyPfxOptions);
    }

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), res -> {
      // Expect connect to succeed
      context.assertTrue(res.succeeded());
      async.complete();
    });

    async.awaitSuccess();

    if(supplyClientCert) {
      context.assertEquals("EXTERNAL", authenticator.getChosenMech());
      SSLSession sslSession = authenticator.getClientSocket().sslSession();
      context.assertNotNull(sslSession.getPeerPrincipal());
    } else {
      context.assertEquals("ANONYMOUS", authenticator.getChosenMech());
    }
  }

  private ProtonServer createTestServer(ProtonServerOptions serverOptions,
      TestExternalAuthenticator authenticator) throws InterruptedException,
                                                                                 ExecutionException {
    ProtonServer server = ProtonServer.create(vertx, serverOptions);
    server.connectHandler(serverConnection -> {
      serverConnection.closeHandler(x -> {
        serverConnection.close();
        serverConnection.disconnect();
      });

      serverConnection.openHandler(y -> {
        serverConnection.open();
      });
    });

    server.saslAuthenticatorFactory(() -> authenticator);
    Promise<ProtonServer> promise = Promise.promise();
    server.listen(0, ar -> {
      if (ar.succeeded()) {
        promise.complete(ar.result());
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future().await();
  }

  private static final class TestExternalAuthenticator implements ProtonSaslAuthenticator {
    private Sasl sasl;
    private String[] offeredMechs;
    private String chosenMech = null;
    boolean succeeded = false;
    NetSocket socket;

    public TestExternalAuthenticator(String... offeredMechs){
      this.offeredMechs = offeredMechs;
    }

    @Override
    public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
      this.socket = socket;
      this.sasl = transport.sasl();
      sasl.server();
      sasl.allowSkip(false);
      sasl.setMechanisms(offeredMechs);
    }

    @Override
    public void process(Handler<Boolean> processComplete) {
      boolean done = false;
      String[] remoteMechanisms = sasl.getRemoteMechanisms();
      if (remoteMechanisms.length > 0) {
        chosenMech = remoteMechanisms[0];

        byte[] initialResponse = new byte[sasl.pending()];
        sasl.recv(initialResponse, 0, initialResponse.length);

        sasl.done(SaslOutcome.PN_SASL_OK);
        succeeded = true;

        done = true;
      }

      processComplete.handle(done);
    }

    @Override
    public boolean succeeded() {
      return succeeded;
    }

    public String getChosenMech() {
      return chosenMech;
    }

    public NetSocket getClientSocket() {
      return socket;
    }
  }
}
