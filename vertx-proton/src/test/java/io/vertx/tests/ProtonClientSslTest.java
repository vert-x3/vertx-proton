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

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.spi.tls.SslContextFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.util.concurrent.ExecutionException;

@RunWith(VertxUnitRunner.class)
public class ProtonClientSslTest {

  private static Logger LOG = LoggerFactory.getLogger(ProtonClientSslTest.class);

  private static final String PASSWORD = "password";
  private static final String KEYSTORE = "src/test/resources/broker-pkcs12.keystore";
  private static final String WRONG_HOST_KEYSTORE = "src/test/resources/broker-wrong-host-pkcs12.keystore";
  private static final String TRUSTSTORE = "src/test/resources/client-pkcs12.truststore";
  private static final String KEYSTORE_UPDATED = "src/test/resources/broker-updated-pkcs12.keystore";
  private static final String TRUSTSTORE_UPDATED = "src/test/resources/client-updated-pkcs12.truststore";
  private static final String KEYSTORE_CLIENT = "src/test/resources/client-pkcs12.keystore";
  private static final String OTHER_CA_TRUSTSTORE = "src/test/resources/other-ca-pkcs12.truststore";
  private static final String VERIFY_HTTPS = "HTTPS";
  private static final String NO_VERIFY = "";

  private Vertx vertx;
  private ProtonServer protonServer;

  @Before
  public void setup() {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown() {
    try {
      vertx.close();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testConnectWithSslSucceeds(TestContext context) throws Exception {
    Async async = context.async();

    // Create a server that accept a connection and expects a client connection+session+receiver
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(true);
    PfxOptions serverPfxOptions = new PfxOptions().setPath(KEYSTORE).setPassword(PASSWORD);
    serverOptions.setKeyCertOptions(serverPfxOptions);

    protonServer = createServer(serverOptions, this::handleClientConnectionSessionReceiverOpen);

    // Connect the client and open a receiver to verify the connection works
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);
    PfxOptions clientPfxOptions = new PfxOptions().setPath(TRUSTSTORE).setPassword(PASSWORD);
    clientOptions.setTrustOptions(clientPfxOptions);

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), res -> {
      // Expect connect to succeed
      context.assertTrue(res.succeeded());
      ProtonConnection connection = res.result();
      connection.open();

      ProtonReceiver receiver = connection.createReceiver("some-address");

      receiver.openHandler(recvResult -> {
        context.assertTrue(recvResult.succeeded());
        LOG.trace("Client reciever open");
        async.complete();
      }).open();
    });

    async.awaitSuccess();
  }

  // This test is here to cover a WildFly use case for passing in an SSLContext for which there are no
  // configuration options.
  // This is currently done by casing to ProtonClientImpl and calling setSuppliedSSLContext().
  @Test(timeout = 20000)
  public void testConnectWithSuppliedSslContextSucceeds(TestContext context) throws Exception {
    Async async = context.async();

    // Create a server that accept a connection and expects a client connection+session+receiver
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(true);
    PfxOptions serverPfxOptions = new PfxOptions().setPath(KEYSTORE).setPassword(PASSWORD);
    serverOptions.setKeyCertOptions(serverPfxOptions);

    protonServer = createServer(serverOptions, this::handleClientConnectionSessionReceiverOpen);

    // Connect the client and open a receiver to verify the connection works
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);

    Path tsPath = Paths.get(".").resolve(TRUSTSTORE);
    TrustManagerFactory tmFactory;
    try (InputStream trustStoreStream = Files.newInputStream(tsPath, StandardOpenOption.READ)){
      KeyStore trustStore = KeyStore.getInstance("pkcs12");
      trustStore.load(trustStoreStream, PASSWORD.toCharArray());
      tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmFactory.init(trustStore);
    }

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(
      null,
      tmFactory.getTrustManagers(),
      null
    );

    clientOptions.setSslEngineOptions(new JdkSSLEngineOptions() {
      @Override
      public SslContextFactory sslContextFactory() {
        return () -> new JdkSslContext(
          sslContext,
          true,
          null,
          IdentityCipherSuiteFilter.INSTANCE,
          ApplicationProtocolConfig.DISABLED,
          io.netty.handler.ssl.ClientAuth.NONE,
          null,
          false);
      }
    });

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), res -> {
      // Expect connect to succeed
      context.assertTrue(res.succeeded());
      ProtonConnection connection = res.result();
      connection.open();

      ProtonReceiver receiver = connection.createReceiver("some-address");

      receiver.openHandler(recvResult -> {
        context.assertTrue(recvResult.succeeded());
        LOG.trace("Client reciever open");
        async.complete();
      }).open();
    });

    async.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testConnectWithSslToNonSslServerFails(TestContext context) throws Exception {
    Async async = context.async();

    // Create a server that doesn't use ssl
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(false);

    protonServer = createServer(serverOptions, this::handleClientConnectionSessionReceiverOpen);

    // Try to connect the client and expect it to fail
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);
    PfxOptions pfxOptions = new PfxOptions().setPath(TRUSTSTORE).setPassword(PASSWORD);
    clientOptions.setTrustOptions(pfxOptions);

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), res -> {
      // Expect connect to fail due to remote peer not doing SSL
      context.assertFalse(res.succeeded());
      async.complete();
    });

    async.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testConnectWithSslToServerWithUntrustedKeyFails(TestContext context) throws Exception {
    Async async = context.async();

    // Create a server that accept a connection and expects a client connection+session+receiver
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(true);
    PfxOptions serverPfxOptions = new PfxOptions().setPath(KEYSTORE).setPassword(PASSWORD);
    serverOptions.setKeyCertOptions(serverPfxOptions);

    protonServer = createServer(serverOptions, this::handleClientConnectionSessionReceiverOpen);

    // Try to connect the client and expect it to fail due to us not trusting the server
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);
    PfxOptions pfxOptions = new PfxOptions().setPath(OTHER_CA_TRUSTSTORE).setPassword(PASSWORD);
    clientOptions.setTrustOptions(pfxOptions);

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), res -> {
      // Expect connect to fail due to remote peer not doing SSL
      context.assertFalse(res.succeeded());
      async.complete();
    });

    async.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testConnectWithSslToServerWhileUsingTrustAll(TestContext context) throws Exception {
    Async async = context.async();

    // Create a server that accept a connection and expects a client connection+session+receiver
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(true);
    PfxOptions serverPfxOptions = new PfxOptions().setPath(KEYSTORE).setPassword(PASSWORD);
    serverOptions.setKeyCertOptions(serverPfxOptions);

    protonServer = createServer(serverOptions, this::handleClientConnectionSessionReceiverOpen);

    // Try to connect the client and expect it to succeed due to trusting all certs
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);
    clientOptions.setTrustAll(true);

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), res -> {
      // Expect connect to succeed
      context.assertTrue(res.succeeded());
      async.complete();
    });

    async.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testConnectWithSslWithoutRequiredClientKeyFails(TestContext context) throws Exception {
    doClientCertificateTestImpl(context, false);
  }

  @Test(timeout = 20000)
  public void testConnectWithSslWithRequiredClientKeySucceeds(TestContext context) throws Exception {
    doClientCertificateTestImpl(context, true);
  }

  private void doClientCertificateTestImpl(TestContext context, boolean supplyClientCert) throws InterruptedException,
                                                                                          ExecutionException {
    Async async = context.async();

    // Create a server that accept a connection and expects a client connection+session+receiver
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(true);
    serverOptions.setClientAuth(ClientAuth.REQUIRED);
    PfxOptions serverPfxOptions = new PfxOptions().setPath(KEYSTORE).setPassword(PASSWORD);
    serverOptions.setKeyCertOptions(serverPfxOptions);

    PfxOptions pfxOptions = new PfxOptions().setPath(TRUSTSTORE).setPassword(PASSWORD);
    serverOptions.setTrustOptions(pfxOptions);

    protonServer = createServer(serverOptions, this::handleClientConnectionSessionReceiverOpen);

    // Try to connect the client
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);
    clientOptions.setTrustOptions(pfxOptions);

    if (supplyClientCert) {
      PfxOptions clientKeyPfxOptions = new PfxOptions().setPath(KEYSTORE_CLIENT).setPassword(PASSWORD);
      clientOptions.setKeyCertOptions(clientKeyPfxOptions);
    }

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), res -> {
      if (supplyClientCert) {
        // Expect connect to succeed
        context.assertTrue(res.succeeded());
      } else {
        // Expect connect to fail
        context.assertFalse(res.succeeded());
      }
      async.complete();
    });

    async.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testConnectWithHostnameVerification(TestContext context) throws Exception {
    doHostnameVerificationTestImpl(context, true);
  }

  @Test(timeout = 20000)
  public void testConnectWithoutHostnameVerification(TestContext context) throws Exception {
    doHostnameVerificationTestImpl(context, false);
  }

  private void doHostnameVerificationTestImpl(TestContext context, boolean verifyHost) throws Exception {

    Async async = context.async();

    // Create a server that accept a connection and expects a client connection+session+receiver
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(true);
    PfxOptions serverPfxOptions = new PfxOptions().setPath(WRONG_HOST_KEYSTORE).setPassword(PASSWORD);
    serverOptions.setKeyCertOptions(serverPfxOptions);

    protonServer = createServer(serverOptions, this::handleClientConnectionSessionReceiverOpen);

    // Connect the client and open a receiver to verify the connection works
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);
    PfxOptions clientPfxOptions = new PfxOptions().setPath(TRUSTSTORE).setPassword(PASSWORD);
    clientOptions.setTrustOptions(clientPfxOptions);

    // Verify/update the hostname verification settings
    context.assertEquals(VERIFY_HTTPS, clientOptions.getHostnameVerificationAlgorithm(),
        "expected host verification to be on by default");
    if (!verifyHost) {
      clientOptions.setHostnameVerificationAlgorithm(NO_VERIFY);
    }

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), res -> {
      if (verifyHost) {
        // Expect connect to fail as server cert hostname doesn't match.
        context.assertFalse(res.succeeded(), "expected connect to fail");
        LOG.trace("Connect failed");
        async.complete();
      } else {
        // Expect connect to succeed as verification is disabled
        context.assertTrue(res.succeeded(), "expected connect to succeed");
        LOG.trace("Connect succeeded");
        ProtonConnection connection = res.result();
        connection.open();

        ProtonReceiver receiver = connection.createReceiver("some-address");

        receiver.openHandler(recvResult -> {
          context.assertTrue(recvResult.succeeded());
          LOG.trace("Client receiver open");
          async.complete();
        }).open();
      }
    });

    async.awaitSuccess();
  }

  /**
   * This test is here to cover update server SSL Options. Historical connections are not affected.
   * New connections are successfully connected using the new trustStore.
   */
  @Test(timeout = 20000)
  public void testUpdateSSLOptionsSuccess(TestContext context) throws Exception {
    doTestUpdateSSLOptions(context, true);
  }

  @Test(timeout = 20000)
  public void testUpdateSSLOptionsNewConnectionWithOldTrustStoreFail(TestContext context) throws Exception {
    doTestUpdateSSLOptions(context, false);
  }

  private void doTestUpdateSSLOptions(TestContext context, boolean isClientUsingNewTrustStore) throws Exception {
    Async async = context.async();

    // Create a server that accept a connection and expects a client connection+session+receiver
    ProtonServerOptions serverOptions = createServerOptionsByKeyStorePath(KEYSTORE);

    protonServer = createServer(serverOptions, this::handleClientConnectionSessionReceiverOpen);

    // Connect the client and open a receiver to verify the connection works
    ProtonClientOptions clientOptions = createClientOptionsByTrustStorePath(TRUSTSTORE);

    ProtonClient client = ProtonClient.create(vertx);
    client.connect(clientOptions, "localhost", protonServer.actualPort(), context.asyncAssertSuccess(connection -> {
      // Don't expect the connection disconnect when update server ssl options
      connection.disconnectHandler(protonConnection -> {
        if (!async.isCompleted()) {
          context.fail("connection close");
        }
      }).open();

      ProtonReceiver receiver = connection.createReceiver("some-address");

      receiver.openHandler(context.asyncAssertSuccess(recv -> {
        LOG.trace("Client receiver open");
        protonServer.updateSSLOptions(createServerOptionsByKeyStorePath(KEYSTORE_UPDATED), false,
          context.asyncAssertSuccess(server -> {
            if (isClientUsingNewTrustStore) {
              // the connection is successfully connected using new truestStore
              createNewClientByTrustStorePath(client, async, context, TRUSTSTORE_UPDATED, true);
            } else {
              // the connection is fails to connected using old trustStore
              createNewClientByTrustStorePath(client, async, context, TRUSTSTORE, false);
            }
          }));
      })).closeHandler(protonReceiver -> context.fail("receiver close")).open();
    }));

    async.awaitSuccess();
  }

  private void createNewClientByTrustStorePath(ProtonClient client, Async async, TestContext context, String trustStorePath, boolean expectSuccess) {
    if (!expectSuccess) {
      client.connect(createClientOptionsByTrustStorePath(trustStorePath), "localhost", protonServer.actualPort(),
        context.asyncAssertFailure(connection -> async.complete()));
      return;
    }

    client.connect(createClientOptionsByTrustStorePath(trustStorePath), "localhost", protonServer.actualPort(),
      context.asyncAssertSuccess(connection -> {
        connection.open();

        ProtonReceiver receiver = connection.createReceiver("some-address");

        receiver.openHandler(context.asyncAssertSuccess(rece -> {
          LOG.trace("Client receiver open");
          async.complete();
        })).open();
      }));
  }

  private ProtonClientOptions createClientOptionsByTrustStorePath(String trustStorePath) {
    ProtonClientOptions clientOptions = new ProtonClientOptions();
    clientOptions.setSsl(true);
    PfxOptions clientPfxOptions = new PfxOptions().setPath(trustStorePath).setPassword(PASSWORD);
    clientOptions.setTrustOptions(clientPfxOptions);
    return clientOptions;
  }

  private ProtonServerOptions createServerOptionsByKeyStorePath(String keyStorePath) {
    ProtonServerOptions serverOptions = new ProtonServerOptions();
    serverOptions.setSsl(true);
    PfxOptions serverPfxOptions = new PfxOptions().setPath(keyStorePath).setPassword(PASSWORD);
    serverOptions.setKeyCertOptions(serverPfxOptions);
    return serverOptions;
  }

  private ProtonServer createServer(ProtonServerOptions serverOptions,
                                    Handler<ProtonConnection> serverConnHandler) throws InterruptedException,
                                                                                 ExecutionException {
    ProtonServer server = ProtonServer.create(vertx, serverOptions);

    server.connectHandler(serverConnHandler);

    FutureHandler<ProtonServer, AsyncResult<ProtonServer>> handler = FutureHandler.asyncResult();
    server.listen(0, handler);
    handler.get();

    return server;
  }

  private void handleClientConnectionSessionReceiverOpen(ProtonConnection serverConnection) {
    // Expect a session to open, when the receiver is created by the client
    serverConnection.sessionOpenHandler(serverSession -> {
      LOG.trace("Server session open");
      serverSession.open();
    });
    // Expect a sender link, then close the session after opening it.
    serverConnection.senderOpenHandler(serverSender -> {
      LOG.trace("Server sender open");
      serverSender.open();
    });

    serverConnection.openHandler(serverSender -> {
      LOG.trace("Server connection open");
      serverConnection.open();
    });
  }
}
