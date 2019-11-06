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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.impl.ProtonConnectionImpl;
import io.vertx.proton.impl.ProtonMetaDataSupportImpl;
import io.vertx.proton.impl.ProtonServerImpl;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedLong;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.codec.WritableBuffer;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.vertx.proton.ProtonHelper.message;

@RunWith(VertxUnitRunner.class)
public class ProtonClientTest extends MockServerTestBase {

  private static Logger LOG = LoggerFactory.getLogger(ProtonClientTest.class);

  private static final Symbol[] ANON_RELAY_ONLY = {ProtonConnectionImpl.ANONYMOUS_RELAY};
  private static final Symbol[] NO_CAPABILITIES = new Symbol[0];

  @Test(timeout = 20000)
  public void testConnectionOpenResultReturnsConnection(TestContext context) {
    Async async = context.async();
    connect(context, connectedConn -> {
      connectedConn.openHandler(result -> {
        context.assertTrue(result.succeeded());

        ProtonConnection openedConn = result.result();
        context.assertNotNull(openedConn, "opened connection result should not be null");
        openedConn.disconnect();
        async.complete();
      }).open();
    });
  }

  @Test(timeout = 20000)
  public void testConnectionDisconnectedDuringCreation(TestContext context) {
    server.close();

    Async connectFailsAsync = context.async();

    NetServer netServer = this.vertx.createNetServer();
    netServer.connectHandler(netSocket -> {
      netSocket.pause();
      vertx.setTimer(50, x -> {
        netSocket.close();
      });
    });

    netServer.listen(listenResult -> {
      context.assertTrue(listenResult.succeeded());

      ProtonClient.create(vertx).connect("localhost", netServer.actualPort(), connResult -> {
        context.assertFalse(connResult.succeeded());
        connectFailsAsync.complete();
      });

    });

    connectFailsAsync.awaitSuccess();
  }

  @Test(timeout = 20000)
  public void testConnectionOpenWithFutureConnect(TestContext context) {
    Async connectedAsync = context.async();
    Async openedAsync = context.async();

    ProtonClient client = ProtonClient.create(vertx);
    Future<ProtonConnection> future = client.connect("localhost", server.actualPort());

    future.onFailure(x -> {
      context.fail("Connect failed, cause: " + x);
    });

    future.onSuccess(conn -> {
      context.assertNotNull(conn, "connection result should not be null");
      connectedAsync.complete();

      conn.openHandler(res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection openedConn = res.result();
        context.assertNotNull(openedConn, "opened connection result should not be null");
        openedConn.disconnect();
        openedAsync.complete();
      }).open();
    });
  }

  @Test(timeout = 20000)
  public void testConnectionDisconnectedDuringCreationWithFutureConnect(TestContext context) {
    server.close();

    Async connectFailsAsync = context.async();

    NetServer netServer = this.vertx.createNetServer();
    netServer.connectHandler(netSocket -> {
      netSocket.pause();
      vertx.setTimer(50, x -> {
        netSocket.close();
      });
    });

    netServer.listen(listenResult -> {
      context.assertTrue(listenResult.succeeded());

      ProtonClient client = ProtonClient.create(vertx);
      Future<ProtonConnection> future = client.connect("localhost", netServer.actualPort());

      future.onFailure(x -> {
        connectFailsAsync.complete();
      });

      future.onSuccess(conn -> {
        context.fail("Connect succeeded when not expected to");
      });
    });
  }

  @Test(timeout = 20000)
  public void testGetConnectionFromLink(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      // Don't need to open connection, just create the objects

      //Check with the connections default session
      ProtonSender defaultSessionSender = connection.createSender("some-address");
      context.assertNotNull(defaultSessionSender);

      // Verify the connection is returned as expected (equals and same object)
      context.assertEquals(connection, defaultSessionSender.getSession().getConnection());
      context.assertTrue(connection == defaultSessionSender.getSession().getConnection());

      //Check with explicitly created session
      ProtonSession session = connection.createSession();
      context.assertNotNull(session);
      ProtonSender sender = session.createSender("some-address");
      context.assertNotNull(sender);

      // Verify the session is returned as expected (equals and same object)
      context.assertEquals(session, sender.getSession());
      context.assertTrue(session == sender.getSession());

      // Verify the connection is returned as expected (equals and same object)
      context.assertEquals(connection, sender.getSession().getConnection());
      context.assertTrue(connection == sender.getSession().getConnection());

      connection.disconnect();
      async.complete();
    });
  }

  @Test(timeout = 20000)
  public void testClientIdentification(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.setContainer("foo").openHandler(x -> {
        context.assertEquals("foo", connection.getContainer());
        // Our mock server responds with a pong container id
        context.assertEquals("pong: foo", connection.getRemoteContainer());
        connection.disconnect();
        async.complete();
      }).open();
    });
  }

  @Test(timeout = 20000)
  public void testRemoteDisconnectHandling(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      context.assertFalse(connection.isDisconnected());
      connection.disconnectHandler(x -> {
        context.assertTrue(connection.isDisconnected());
        LOG.trace("Client disconnect handler called");
        async.complete();
      });

      // Send a request to the server for him to disconnect us
      ProtonSender sender = connection.createSender(null).open();
      LOG.trace("Sending request for remote disconnect");
      sender.send(message("command", "disconnect"));
    });
  }

  @Test(timeout = 20000)
  public void testLocalDisconnectHandling(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      context.assertFalse(connection.isDisconnected());
      connection.disconnectHandler(x -> {
        LOG.trace("Client disconnect handler called");
        context.assertTrue(connection.isDisconnected());
        async.complete();
      });
      // We will force the disconnection to the server
      LOG.trace("Client disconnecting connection");
      connection.disconnect();
    });
  }

  @Test(timeout = 20000)
  public void testSetVirtualHostOnConnect(TestContext context) {
    Async async = context.async();
    ProtonClientOptions options = new ProtonClientOptions()
      .setVirtualHost("example.com");
    connect(context, options, connection -> {
      context.assertFalse(connection.isDisconnected());
      context.assertEquals("example.com", connection.getHostname());
      async.complete();
    });
  }

  @Test(timeout = 20000)
  public void testRequestResponse(TestContext context) {
    sendReceiveEcho(context, "Hello World");
  }

  @Test(timeout = 20000)
  public void testTransferLargeMessage(TestContext context) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 1024 * 1024 * 5; i++) {
      builder.append('a' + (i % 26));
    }
    sendReceiveEcho(context, builder.toString());
  }

  @Test(timeout = 20000)
  public void testTransferLargeMessageWithSmallerSessionWindow(TestContext context) {
    Async async = context.async();

    int msgContentSize = 5 * 1024 * 1024;
    int windowCapacity = 2 * 1024 * 1024;
    context.assertTrue( msgContentSize >= 2 * windowCapacity);

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < msgContentSize; i++) {
      builder.append('a' + (i % 26));
    }
    String content = builder.toString();

    ProtonClient client = ProtonClient.create(vertx);
    client.connect("localhost", server.actualPort(), res -> {
      context.assertTrue(res.succeeded());
      ProtonConnection connection = res.result();
      connection.open();

      // Set up receiver on session with window of limited capacity
      final ProtonSession session = connection.createSession();
      session.setIncomingCapacity(windowCapacity);
      session.open();

      final ProtonReceiver receiver = session.createReceiver(MockServer.Addresses.echo.toString());
      receiver.handler((d, m) -> {
        LOG.trace("Got message");
        String actual = (String) (getMessageBody(context, m));
        context.assertEquals(content, actual);

        async.complete();
        connection.disconnect();
      });
      receiver.open();

      // Now send the message
      session.createSender(MockServer.Addresses.echo.toString()).open().send(message("echo", content));
    });
  }

  private void sendReceiveEcho(TestContext context, String data) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      connection.createReceiver(MockServer.Addresses.echo.toString()).handler((d, m) -> {
        String actual = (String) (getMessageBody(context, m));
        context.assertEquals(data, actual);
        connection.disconnect();
        async.complete();
      }).open();

      connection.createSender(MockServer.Addresses.echo.toString()).open().send(message("echo", data));

    });
  }

  @Test(timeout = 20000)
  public void testIsAnonymousRelaySupported(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      context.assertFalse(connection.isAnonymousRelaySupported(), "Connection not yet open, so result should be false");
      connection.openHandler(x -> {
        context.assertTrue(connection.isAnonymousRelaySupported(),
            "Connection now open, server supports relay, should be true");

        connection.disconnect();
        async.complete();
      }).open();
    });
  }

  @Test(timeout = 20000)
  public void testAnonymousRelayIsNotSupported(TestContext context) {
    ((ProtonServerImpl) server.getProtonServer()).setAdvertiseAnonymousRelayCapability(false);
    Async async = context.async();
    connect(context, connection -> {
      context.assertFalse(connection.isAnonymousRelaySupported(), "Connection not yet open, so result should be false");
      connection.openHandler(x -> {
        context.assertFalse(connection.isAnonymousRelaySupported(),
            "Connection now open, server does not support relay, should be false");

        connection.disconnect();
        async.complete();
      }).open();
    });
  }

  @Test(timeout = 20000)
  public void testAnonymousSenderEnforcesMessageHasAddress(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      ProtonSender sender = connection.createSender(null);
      sender.open();

      Message messageWithNoAddress = Proton.message();
      messageWithNoAddress.setBody(new AmqpValue("bodyString"));
      try {
        sender.send(messageWithNoAddress);
        context.fail("Send should have thrown IAE due to lack of message address");
      } catch (IllegalArgumentException iae) {
        // Expected
        connection.disconnect();
        async.complete();
      }
    });
  }

  @Test(timeout = 20000)
  public void testNonAnonymousSenderDoesNotEnforceMessageHasAddress(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      ProtonSender sender = connection.createSender(MockServer.Addresses.drop.toString());
      sender.open();

      Message messageWithNoAddress = Proton.message();
      messageWithNoAddress.setBody(new AmqpValue("bodyString"));

      sender.send(messageWithNoAddress);
      connection.disconnect();
      async.complete();
    });
  }

  @Test(timeout = 20000)
  public void testDefaultAnonymousSenderSpecifiesLinkTarget(TestContext context) throws Exception {
    server.close();
    Async async = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer(
          (serverConnection) -> processConnectionAnonymousSenderSpecifiesLinkTarget(context, async, serverConnection));

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.openHandler(x -> {
          LOG.trace("Client connection opened");

          ProtonSender sender = connection.createSender(null);
          // Can optionally add an openHandler or sendQueueDrainHandler
          // to await remote sender open completing or credit to send being
          // granted. But here we will just buffer the send immediately.
          sender.open();
          sender.send(message("ignored", "content"));
        }).open();
      });

      async.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  private void processConnectionAnonymousSenderSpecifiesLinkTarget(TestContext context, Async async,
                                                                   ProtonConnection serverConnection) {
    serverConnection.sessionOpenHandler(session -> session.open());
    serverConnection.receiverOpenHandler(receiver -> {
      LOG.trace("Server receiver opened");
      // TODO: set the local target on link before opening it
      receiver.handler((delivery, msg) -> {
        // We got the message that was sent, complete the test
        if (LOG.isTraceEnabled()) {
          LOG.trace("Server got msg: " + getMessageBody(context, msg));
        }
        serverConnection.disconnect();
        async.complete();
      });

      // Verify that the remote link target (set by the client) matches
      // up to the expected value to signal use of the anonymous relay
      Target remoteTarget = receiver.getRemoteTarget();
      context.assertNotNull(remoteTarget, "Client did not set a link target");
      context.assertNull(remoteTarget.getAddress(), "Unexpected target address");

      receiver.open();
    });
    serverConnection.openHandler(result -> {
      serverConnection.open();
    });
  }

  @Test(timeout = 20000)
  public void testConfigureDynamicReceiver(TestContext context) throws Exception {
    server.close();

    final Async clientLinkOpenAsync = context.async();
    final Async serverLinkOpenAsync = context.async();

    final String dynamicAddress = "testConfigureDynamicReceiver:" + UUID.randomUUID();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          serverSender.closeHandler(res -> {
            serverSender.close();
          });

          // Verify the remote terminus details used were as expected
          context.assertNotNull(serverSender.getRemoteSource(), "source should not be null");
          org.apache.qpid.proton.amqp.messaging.Source remoteSource = (org.apache.qpid.proton.amqp.messaging.Source) serverSender.getRemoteSource();
          context.assertTrue(remoteSource.getDynamic(), "expected dynamic source to be requested");
          context.assertNull(remoteSource.getAddress(), "expected no source address to be set");

          // Set the local terminus details
          org.apache.qpid.proton.amqp.messaging.Source source = (org.apache.qpid.proton.amqp.messaging.Source) remoteSource.copy();
          source.setAddress(dynamicAddress);
          serverSender.setSource(source);

          LOG.trace("Server sender opened");
          serverSender.open();

          serverLinkOpenAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        // Create receiver with dynamic option
        ProtonLinkOptions options = new ProtonLinkOptions().setDynamic(true);

        final ProtonReceiver receiver = connection.createReceiver(null, options);

        receiver.openHandler(y -> {
          LOG.trace("Client link opened");
          // Verify the remote address details
          context.assertEquals(dynamicAddress, receiver.getRemoteAddress(), "unexpected remote address");

          // Grab and verify the source details
          org.apache.qpid.proton.amqp.messaging.Source remoteSource = (org.apache.qpid.proton.amqp.messaging.Source) receiver.getRemoteSource();
          context.assertTrue(remoteSource.getDynamic(), "expected dynamic source");
          context.assertEquals(dynamicAddress, remoteSource.getAddress(), "unexpected source address");

          clientLinkOpenAsync.complete();
          connection.disconnect();
        });
        receiver.open();
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkOpenAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testReceiveMultipleMessagesWithLowerPrefetch(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      AtomicInteger counter = new AtomicInteger(0);

      ProtonReceiver receiver = connection.createReceiver(MockServer.Addresses.five_messages.toString());
      // Set prefetch to 2 credit. Test verifies receiver gets multiple messages, i.e credit is being replenished.
      receiver.setPrefetch(2)
      .handler((d, m) -> {
        int count = counter.incrementAndGet();

        validateMessage(context, count, String.valueOf(count), m);

        if (count == 5) {
          // Got the last message, lets finish the test.
          LOG.trace("Got msg 5, completing async");
          async.complete();
          connection.disconnect();
        }
      }).open();
    });
  }

  @Test(timeout = 20000)
  public void testDelayedInitialCreditWithPrefetchDisabled(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      AtomicInteger counter = new AtomicInteger(0);
      AtomicBoolean initialCreditGranted = new AtomicBoolean();
      AtomicBoolean additionalCreditGranted = new AtomicBoolean();
      final int delay = 250;
      final long startTime = System.currentTimeMillis();

      // Create receiver with prefetch disabled
      ProtonReceiver receiver = connection.createReceiver(MockServer.Addresses.two_messages.toString());
      receiver.handler((d, m) -> {
        int count = counter.incrementAndGet();
        switch (count) {
        case 1: {
          validateMessage(context, count, String.valueOf(count), m);

          context.assertTrue(initialCreditGranted.get(),
              "Initial credit not yet granted, so we" + " should not have received message 1 yet!");

          // Verify lack of initial credit results in delayed receipt of first message.
          context.assertTrue(System.currentTimeMillis() > startTime + delay,
              "Message received before expected time delay elapsed!");

          LOG.trace("Got msg 1");

          // We only issued 1 credit, so we should not get more
          // messages until more credit is flowed, use the
          // callback for this msg to do that after further delay
          vertx.setTimer(delay, x -> {
            LOG.trace("Granting additional credit");
            additionalCreditGranted.set(true);
            receiver.flow(1);
          });
          break;
        }
        case 2: {
          validateMessage(context, count, String.valueOf(count), m);
          context.assertTrue(additionalCreditGranted.get(),
              "Additional credit not yet granted, so we" + " should not have received message " + count + " yet!");

          context.assertTrue(System.currentTimeMillis() > startTime + (delay * 2),
              "Message received before expected time delay elapsed!");

          // Got the last message, lets finish the test.
          LOG.trace("Got msg 2, completing async");
          async.complete();
          connection.disconnect();
          break;
        }
        }
      }).setPrefetch(0) // Turn off automatic prefetch / credit handling
          .open();

      // Explicitly grant an initial credit after a delay. Handler will then grant more.
      vertx.setTimer(delay, x -> {
        LOG.trace("Flowing initial credit");
        initialCreditGranted.set(true);
        receiver.flow(1);
      });
    });
  }

  @Test(timeout = 20000)
  public void testImmediateInitialCreditWithPrefetchDisabled(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      AtomicInteger counter = new AtomicInteger(0);
      AtomicBoolean creditGranted = new AtomicBoolean();
      ProtonReceiver receiver = connection.createReceiver(MockServer.Addresses.five_messages.toString());

      receiver.handler((d, m) -> {
        int count = counter.incrementAndGet();
        switch (count) {
        case 1: // Fall-through
        case 2: // Fall-through
        case 3: {
          validateMessage(context, count, String.valueOf(count), m);
          break;
        }
        case 4: {
          validateMessage(context, count, String.valueOf(count), m);

          // We only issued 4 credits, so we should not get
          // more messages until more credit is flowed, use
          // the callback for this msg to do that
          vertx.setTimer(1000, x -> {
            LOG.trace("Flowing more credit");
            creditGranted.set(true);
            receiver.flow(1);
          });

          // Check that we haven't processed any more messages before then
          vertx.setTimer(500, x -> {
            LOG.trace("Checking msg 5 not received yet");
            context.assertEquals(4, counter.get());
          });
          break;
        }
        case 5: {
          validateMessage(context, count, String.valueOf(count), m);
          context.assertTrue(creditGranted.get(),
              "Additional credit not yet granted, so we" + " should not have received message 5 yet!");

          // Got the last message, lets finish the test.
          LOG.trace("Got msg 5, completing async");
          async.complete();
          connection.disconnect();
          break;
        }
        }
      }).setPrefetch(0) // Turn off prefetch and related automatic credit handling
          .flow(4) // Explicitly grant initial credit of 4. Handler will grant more later.
          .open();
    });
  }

  @Test(timeout = 20000)
  public void testDrainWithSomeCreditsUsed(TestContext context) throws Exception {
    doDrainWithSomeCreditUsedTestImpl(context, false);
  }

  @Test(timeout = 20000)
  public void testDrainWithSomeCreditsUsedSenderAutoDrained(TestContext context) throws Exception {
    doDrainWithSomeCreditUsedTestImpl(context, true);
  }

  private void doDrainWithSomeCreditUsedTestImpl(TestContext context, boolean autoDrainedSender) throws Exception {
    server.close();

    int credits = 5;
    int messages = 2;

    // Set up server that will send 2 messages, and either automatically or explicitly set the link drained after.
    MockServer protonServer = doDrainTestServerSetup(context, autoDrainedSender, !autoDrainedSender, messages);

    Async async = context.async();
    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean drainComplete = new AtomicBoolean();

    ProtonClient client = ProtonClient.create(vertx);
    client.connect("localhost", protonServer.actualPort(), res -> {
      context.assertTrue(res.succeeded());

      ProtonConnection connection = res.result();
      connection.open();

      // Create receiver [with prefetch disabled] against the mock server sending 2 messages
      ProtonReceiver receiver = connection.createReceiver("some-address");
      receiver.handler((d, m) -> {
        int count = counter.incrementAndGet();
        switch (count) {
          case 1: //fall through
          case 2:
            validateMessage(context, count, String.valueOf(count), m);
            context.assertFalse(drainComplete.get(), "Drain should not yet be completed!");
            break;
          default:
            context.fail("Should only get 2 messages");
            break;
        }
      }).setPrefetch(0) // Turn off automatic prefetch / credit handling
          .open();

      // Explicitly drain, granting 5 credits first, so not all are used (we only expect 2 messages).
      receiver.flow(credits);
      receiver.drain(10000, result -> {
        context.assertTrue(result.succeeded(), "Drain should have succeeded");
        context.assertEquals(2, counter.get(), "Drain should not yet be completed! Unexpected message count");
        drainComplete.set(true);
        async.complete();
        connection.disconnect();
      });
    });
  }

  @Test(timeout = 20000)
  public void testDrainWithAllCreditsUsed(TestContext context) throws Exception {
    server.close();

    int credits = 5;
    int messages = 5;

    // Set up server that will send messages to use all the credit
    MockServer protonServer = doDrainTestServerSetup(context, true, false, messages);

    Async async = context.async();
    AtomicInteger counter = new AtomicInteger(0);
    AtomicBoolean drainComplete = new AtomicBoolean();

    ProtonClient client = ProtonClient.create(vertx);
    client.connect("localhost", protonServer.actualPort(), res -> {
      context.assertTrue(res.succeeded());

      ProtonConnection connection = res.result();
      connection.open();

      // Create receiver [with prefetch disabled] against the mock server sending 5 messages
      ProtonReceiver receiver = connection.createReceiver("some-address");
      receiver.handler((d, m) -> {
        int count = counter.incrementAndGet();
        switch (count) {
          case 1: //fall through
          case 2: //fall through
          case 3: //fall through
          case 4: //fall through
          case 5:
            validateMessage(context, count, String.valueOf(count), m);
            context.assertFalse(drainComplete.get(), "Drain should not yet be completed!");
            break;
          default:
            context.fail("Should only get 5 messages");
            break;
        }
      }).setPrefetch(0) // Turn off automatic prefetch / credit handling
          .open();

      // Explicitly drain, grant 5 credits, expect all to be used so drain completes without 'drain response' flow.
      receiver.flow(credits);
      receiver.drain(10000, result -> {
        context.assertTrue(result.succeeded(), "Drain should have succeeded");
        context.assertEquals(credits, counter.get(), "Drain should not yet be completed! Unexpected message count");
        drainComplete.set(true);
        async.complete();
        connection.disconnect();
      });
    });
  }

  @Test(timeout = 20000)
  public void testDrainWithNoCredit(TestContext context) {
    Async async = context.async();

    connect(context, connection -> {
      connection.open();

      // Create receiver [with prefetch disabled] against address that will send no messages
      ProtonReceiver receiver = connection.createReceiver(MockServer.Addresses.no_messages.toString());
      receiver.setPrefetch(0) // Turn off automatic prefetch / credit handling
      .open();

      // Explicitly drain, granting no credit first, should no-op
      receiver.drain(0, result -> {
        context.assertTrue(result.succeeded(), "Drain should have succeeded");
        async.complete();
        connection.disconnect();
      });
    });
  }

  @Test(timeout = 20000)
  public void testDrainTimeout(TestContext context) throws Exception {
    server.close();

    long timeout = 1000;

    // Set up server that will send no messages, and won't either automatically or explicitly set the link drained
    MockServer protonServer = doDrainTestServerSetup(context, false, false, 0);
    Async async = context.async();

    ProtonClient client = ProtonClient.create(vertx);
    client.connect("localhost", protonServer.actualPort(), res -> {
      context.assertTrue(res.succeeded());

      ProtonConnection connection = res.result();
      connection.open();

      // Create receiver [with prefetch disabled] against our server that will send no messages
      ProtonReceiver receiver = connection.createReceiver("some-address");
      receiver.setPrefetch(0) // Turn off automatic prefetch / credit handling
      .open();

      // Explicitly drain, granting credit first, expect to fail after the timeout
      receiver.flow(1);
      long start = System.nanoTime();

      receiver.drain(timeout, result -> {
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        context.assertTrue(result.failed(), "Drain should have failed due to timeout");
        context.assertTrue(elapsed >= timeout , "Timeout fired earlier than expected: " + elapsed);
        async.complete();
        connection.disconnect();
      });
    });
  }

  private MockServer doDrainTestServerSetup(TestContext context, boolean autoDrained, boolean explicitDrained,
                                            int messageCount) throws Exception {
    MockServer server = new MockServer(vertx, serverConnection -> {
      // Expect a connection
      serverConnection.openHandler(serverSender -> {
        LOG.trace("Server connection open");
        // Add a close handler
        serverConnection.closeHandler(x -> {
          serverConnection.close();
        });

        serverConnection.open();
      });

      // Expect a session to open, when the receiver is created
      serverConnection.sessionOpenHandler(serverSession -> {
        LOG.trace("Server session open");
        serverSession.open();
      });

      // Expect a sender link open for the client receiver
      serverConnection.senderOpenHandler(serverSender -> {
        if(!autoDrained) {
          serverSender.setAutoDrained(false);
        }

        LOG.trace("Server sender open");
        Source remoteSource = (Source) serverSender.getRemoteSource();
        context.assertNotNull(remoteSource, "source should not be null");
        // Naive test-only handling
        serverSender.setSource(remoteSource.copy());

        serverSender.open();

        serverSender.sendQueueDrainHandler(s -> {
          // When the link is drained, send messages and set drained as directed
          if (s.getDrain()){
            for(int i = 1; i <= messageCount; i++){
              serverSender.send(message(String.valueOf(i)));
            }

            if(explicitDrained) {
              serverSender.drained();
            }
          }
        });
      });
    });

    return server;
  }

  @Test(timeout = 20000)
  public void testRemoteCloseDefaultSessionWithError(TestContext context) throws Exception {
    remoteCloseDefaultSessionTestImpl(context, true);
  }

  @Test(timeout = 20000)
  public void testRemoteCloseDefaultSessionWithoutError(TestContext context) throws Exception {
    remoteCloseDefaultSessionTestImpl(context, false);
  }

  private void remoteCloseDefaultSessionTestImpl(TestContext context, boolean sessionError) throws InterruptedException,
                                                                                            ExecutionException {
    server.close();
    Async async = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer(serverConnection -> {
        Promise<ProtonSession> sessionPromise = Promise.<ProtonSession> promise();
        // Expect a session to open, when the sender is created by the client
        serverConnection.sessionOpenHandler(serverSession -> {
          LOG.trace("Server session open");
          serverSession.open();
          sessionPromise.complete(serverSession);
        });
        // Expect a receiver link, then close the session after opening it.
        serverConnection.receiverOpenHandler(serverReceiver -> {
          LOG.trace("Server receiver open");
          serverReceiver.open();

          context.assertTrue(sessionPromise.future().succeeded(), "Session future not [yet] succeeded");
          LOG.trace("Server session close");
          ProtonSession s = sessionPromise.future().result();
          if (sessionError) {
            ErrorCondition error = new ErrorCondition();
            error.setCondition(AmqpError.INTERNAL_ERROR);
            error.setDescription("error description");
            s.setCondition(error);
          }
          s.close();
        });
        serverConnection.openHandler(result -> {
          LOG.trace("Server connection open");
          serverConnection.open();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.openHandler(x -> {
          context.assertTrue(x.succeeded(), "Connection open failed");
          LOG.trace("Client connection opened");

          // Create a sender to provoke creation (and subsequent
          // closure of by the server) the connections default session
          connection.createSender(null).open();
        });
        connection.closeHandler(x -> {
          LOG.trace("Connection close handler called (as espected): " + x.cause());
          async.complete();
        });
        connection.open();
      });

      async.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testReceiverOpenWithAtLeastOnceQos(TestContext context) throws Exception {
    doOpenLinkWithQosTestImpl(context, true, ProtonQoS.AT_LEAST_ONCE);
  }

  @Test(timeout = 20000)
  public void testReceiverOpenWithAtMostOnceQos(TestContext context) throws Exception {
    doOpenLinkWithQosTestImpl(context, true, ProtonQoS.AT_MOST_ONCE);
  }

  @Test(timeout = 20000)
  public void testSenderOpenWithAtLeastOnceQos(TestContext context) throws Exception {
    doOpenLinkWithQosTestImpl(context, false, ProtonQoS.AT_LEAST_ONCE);
  }

  @Test(timeout = 20000)
  public void testSenderOpenWithAtMostOnceQos(TestContext context) throws Exception {
    doOpenLinkWithQosTestImpl(context, false, ProtonQoS.AT_MOST_ONCE);
  }

  public void doOpenLinkWithQosTestImpl(TestContext context, boolean clientSender, ProtonQoS qos) throws Exception {
    server.close();
    Async serverAsync = context.async();
    Async clientAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });
        serverConnection.sessionOpenHandler(session -> session.open());
        if (clientSender) {
          serverConnection.receiverOpenHandler(receiver -> {
            context.assertEquals(qos, receiver.getRemoteQoS(), "unexpected remote qos value");
            LOG.trace("Server receiver opened");
            receiver.open();
            serverAsync.complete();
          });
        } else {
          serverConnection.senderOpenHandler(sender -> {
            context.assertEquals(qos, sender.getRemoteQoS(), "unexpected remote qos value");
            LOG.trace("Server sender opened");
            sender.open();
            serverAsync.complete();
          });
        }
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.openHandler(x -> {
          LOG.trace("Client connection opened");
          final ProtonLink<?> link;
          if (clientSender) {
            link = connection.createSender(null);
          } else {
            link = connection.createReceiver("some-address");
          }
          link.setQoS(qos);

          link.openHandler(y -> {
            LOG.trace("Client link opened");
            context.assertEquals(qos, link.getRemoteQoS(), "unexpected remote qos value");
            clientAsync.complete();
          });
          link.open();

        }).open();
      });

      serverAsync.awaitSuccess();
      clientAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testConnectionPropertiesDefault(TestContext context) throws Exception {
    ConPropValidator defaultExpectedPropsHandler = new ProductVersionConPropValidator(ProtonMetaDataSupportImpl.PRODUCT,
        ProtonMetaDataSupportImpl.VERSION);

    doConnectionPropertiesTestImpl(context, false, null, defaultExpectedPropsHandler, null,
        defaultExpectedPropsHandler);
  }

  @Test(timeout = 20000)
  public void testConnectionPropertiesSetNonDefaultWithoutProductVersion(TestContext context) throws Exception {
    Symbol clientCustomProp = Symbol.valueOf("custom-client-prop-key");
    String clientCustomPropValue = "custom-client-prop-value";

    Symbol serverCustomProp = Symbol.valueOf("custom-server-prop-key");
    String serverCustomPropValue = "custom-server-prop-value";

    LinkedHashMap<Symbol, Object> clientProps = new LinkedHashMap<Symbol, Object>();
    clientProps.put(clientCustomProp, clientCustomPropValue);

    LinkedHashMap<Symbol, Object> serverProps = new LinkedHashMap<Symbol, Object>();
    serverProps.put(serverCustomProp, serverCustomPropValue);

    final ConPropValidator serverExpectedPropsHandler = (c, props) -> {
      new ProductVersionConPropValidator(ProtonMetaDataSupportImpl.PRODUCT, ProtonMetaDataSupportImpl.VERSION)
          .validate(c, props);

      context.assertTrue(props.containsKey(clientCustomProp), "custom client prop not present");
      context.assertEquals(clientCustomPropValue, props.get(clientCustomProp), "unexpected custom client prop value");
    };

    final ConPropValidator clientExpectedPropsHandler = (c, props) -> {
      new ProductVersionConPropValidator(ProtonMetaDataSupportImpl.PRODUCT, ProtonMetaDataSupportImpl.VERSION)
          .validate(c, props);

      context.assertTrue(props.containsKey(serverCustomProp), "custom server prop not present");
      context.assertEquals(serverCustomPropValue, props.get(serverCustomProp), "unexpected custom server prop value");
    };

    doConnectionPropertiesTestImpl(context, true, clientProps, serverExpectedPropsHandler, serverProps,
        clientExpectedPropsHandler);
  }

  @Test(timeout = 20000)
  public void testConnectionPropertiesSetWithCustomProductVersion(TestContext context) throws Exception {
    String customProduct = "custom-product";
    String customVersion = "0.1.2.3.custom";

    LinkedHashMap<Symbol, Object> props = new LinkedHashMap<Symbol, Object>();
    props.put(ProtonMetaDataSupportImpl.PRODUCT_KEY, customProduct);
    props.put(ProtonMetaDataSupportImpl.VERSION_KEY, customVersion);

    ConPropValidator expectedPropsHandler = new ProductVersionConPropValidator(customProduct, customVersion);

    doConnectionPropertiesTestImpl(context, true, props, expectedPropsHandler, props, expectedPropsHandler);
  }

  @Test(timeout = 20000)
  public void testConnectionPropertiesSetExplicitNull(TestContext context) throws Exception {

    final ConPropValidator nullExpectedPropsHandler = (c, props) -> {
      context.assertNull(props, "expected no properties map");
    };

    doConnectionPropertiesTestImpl(context, true, null, nullExpectedPropsHandler, null, nullExpectedPropsHandler);
  }

  public void doConnectionPropertiesTestImpl(TestContext context, boolean setProperties,
                                             Map<Symbol, Object> clientGivenProps,
                                             ConPropValidator serverExpectedPropsHandler,
                                             Map<Symbol, Object> serverGivenProps,
                                             ConPropValidator clientExpectedPropsHandler) throws Exception {
    server.close();
    Async serverAsync = context.async();
    Async clientAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(x -> {
          if (setProperties) {
            serverConnection.setProperties(serverGivenProps);
          }

          serverExpectedPropsHandler.validate(context, serverConnection.getRemoteProperties());

          serverConnection.open();

          serverAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection clientConnection = res.result();
        if (setProperties) {
          clientConnection.setProperties(clientGivenProps);
        }
        clientConnection.openHandler(x -> {
          context.assertTrue(x.succeeded());

          LOG.trace("Client connection opened");
          clientExpectedPropsHandler.validate(context, clientConnection.getRemoteProperties());
          clientAsync.complete();
        }).open();
      });

      serverAsync.awaitSuccess();
      clientAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  private interface ConPropValidator {
    void validate(TestContext context, Map<Symbol, Object> props);
  }

  private class ProductVersionConPropValidator implements ConPropValidator {
    private String expectedProduct;
    private String expectedVersion;

    public ProductVersionConPropValidator(String expectedProduct, String expectedVersion) {
      this.expectedProduct = expectedProduct;
      this.expectedVersion = expectedVersion;
    }

    @Override
    public void validate(TestContext context, Map<Symbol, Object> props) {
      context.assertNotNull(props, "no properties map provided");

      context.assertTrue(props.containsKey(ProtonMetaDataSupportImpl.PRODUCT_KEY), "product not present");
      context.assertNotNull(props.get(ProtonMetaDataSupportImpl.VERSION_KEY), "unexpected product");
      context.assertEquals(expectedProduct, props.get(ProtonMetaDataSupportImpl.PRODUCT_KEY), "unexpected product");

      context.assertTrue(props.containsKey(ProtonMetaDataSupportImpl.VERSION_KEY), "version not present");
      context.assertNotNull(props.get(ProtonMetaDataSupportImpl.VERSION_KEY), "unexpected version");
      context.assertEquals(expectedVersion, props.get(ProtonMetaDataSupportImpl.VERSION_KEY), "unexpected version");
    }

  }

  @Test(timeout = 20000)
  public void testDesiredAndOfferedCapabilities(TestContext context) throws Exception {
    Symbol foo = Symbol.valueOf("foo");
    Symbol bar = Symbol.valueOf("bar");
    Symbol baz = Symbol.valueOf("baz");


    doTestCapabilities(context, new Symbol[] { foo, bar }, new Symbol[] { baz },
                       new Symbol[] { baz }, new Symbol[] { bar }, true);

  }


  @Test(timeout = 20000)
  public void testNotSettingDesiredAndOfferedCapabilitiesRetainsAnonRelay(TestContext context) throws Exception {

    Symbol foo = Symbol.valueOf("foo");


    doTestCapabilities(context, null, new Symbol[] { foo },
                       new Symbol[] { foo }, null, false);

    doTestCapabilities(context, null, new Symbol[] { foo },
                       new Symbol[] { foo }, null, true);

  }


  private void doTestCapabilities(TestContext context, Symbol[] clientDesired, Symbol[] clientOffered,
                                  Symbol[] serverDesired, Symbol[] serverOffered,
                                  boolean setServerOffered) throws Exception {
    server.close();
    Async serverAsync = context.async();
    Async clientAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(x -> {

          serverConnection.setDesiredCapabilities(serverDesired);
          // if anon relay then don't overwrite with null
          if (setServerOffered) {
            serverConnection.setOfferedCapabilities(serverOffered);
          }

          context.assertTrue(Arrays.equals(clientOffered == null ? NO_CAPABILITIES : clientOffered,
                                           serverConnection.getRemoteOfferedCapabilities()));
          context.assertTrue(Arrays.equals(clientDesired == null ? NO_CAPABILITIES : clientDesired,
                                           serverConnection.getRemoteDesiredCapabilities()));

          serverConnection.open();

          serverAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection clientConnection = res.result();

        clientConnection.setOfferedCapabilities(clientOffered);

        clientConnection.setDesiredCapabilities(clientDesired);


        clientConnection.openHandler(x -> {
          context.assertTrue(x.succeeded());

          LOG.trace("Client connection opened");

          context.assertTrue(Arrays.equals(serverDesired == null ? NO_CAPABILITIES : serverDesired,
                                           clientConnection.getRemoteDesiredCapabilities()));
          final Symbol[] expectedServerOffered = setServerOffered ? NO_CAPABILITIES : ANON_RELAY_ONLY;
          context.assertTrue(Arrays.equals(serverOffered == null ? expectedServerOffered : serverOffered,
                                           clientConnection.getRemoteOfferedCapabilities()));



          clientAsync.complete();
        }).open();
      });

      serverAsync.awaitSuccess();
      clientAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }


  @Test(timeout = 20000)
  public void testDetachHandlingWithSender(TestContext context) throws Exception {
    doDetachHandlingTestImpl(context, true);
  }

  @Test(timeout = 20000)
  public void testDetachHandlingWithReceiver(TestContext context) throws Exception {
    doDetachHandlingTestImpl(context, false);
  }

  public void doDetachHandlingTestImpl(TestContext context, boolean clientSender) throws Exception {
    server.close();

    final Async clientLinkOpenAsync = context.async();
    final Async serverLinkOpenAsync = context.async();
    final Async serverLinkDetachAsync = context.async();
    final Async clientLinkDetachAsync = context.async();
    final AtomicBoolean serverLinkCloseHandlerFired = new AtomicBoolean();
    final AtomicBoolean clientLinkCloseHandlerFired = new AtomicBoolean();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        if (clientSender) {
          serverConnection.receiverOpenHandler(serverReceiver -> {
            LOG.trace("Server receiver opened");
            serverReceiver.open();
            serverLinkOpenAsync.complete();

            serverReceiver.closeHandler(res -> {
              serverLinkCloseHandlerFired.set(true);
            });

            serverReceiver.detachHandler(res -> {
              context.assertTrue(res.succeeded(), "expected non-errored async result");
              serverReceiver.detach();
              serverLinkDetachAsync.complete();
            });
          });
        } else {
          serverConnection.senderOpenHandler(serverSender -> {
            LOG.trace("Server sender opened");
            serverSender.open();
            serverLinkOpenAsync.complete();

            serverSender.closeHandler(res -> {
              serverLinkCloseHandlerFired.set(true);
            });

            serverSender.detachHandler(res -> {
              context.assertTrue(res.succeeded(), "expected non-errored async result");
              serverSender.detach();
              serverLinkDetachAsync.complete();
            });
          });
        }
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.openHandler(x -> {
          LOG.trace("Client connection opened");
          final ProtonLink<?> link;
          if (clientSender) {
            link = connection.createSender(null);
          } else {
            link = connection.createReceiver("some-address");
          }

          link.closeHandler(clientLink -> {
            clientLinkCloseHandlerFired.set(true);
          });

          link.detachHandler(clientLink -> {
            LOG.trace("Client link detached");
            clientLinkDetachAsync.complete();
          });

          link.openHandler(y -> {
            LOG.trace("Client link opened");
            clientLinkOpenAsync.complete();

            link.detach();
          });
          link.open();

        }).open();
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkOpenAsync.awaitSuccess();

      serverLinkDetachAsync.awaitSuccess();
      clientLinkDetachAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }

    context.assertFalse(serverLinkCloseHandlerFired.get(), "server link close handler should not have fired");
    context.assertFalse(clientLinkCloseHandlerFired.get(), "client link close handler should not have fired");
  }

  @Test(timeout = 20000)
  public void testMaxMessageSize(TestContext context) throws Exception {
    server.close();
    Async serverAsync = context.async();
    Async clientAsync = context.async();

    final UnsignedLong clientMaxMsgSize = UnsignedLong.valueOf(54321);
    final UnsignedLong serverMaxMsgSize = UnsignedLong.valueOf(12345);

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });
        serverConnection.sessionOpenHandler(session -> {
          session.open();
        });

        serverConnection.senderOpenHandler(serverSender -> {
          context.assertEquals(clientMaxMsgSize, serverSender.getRemoteMaxMessageSize(),
              "unexpected remote max message size at server");
          context.assertNull(serverSender.getMaxMessageSize(), "Expected no value to be set");
          serverSender.setMaxMessageSize(serverMaxMsgSize);
          context.assertEquals(serverMaxMsgSize, serverSender.getMaxMessageSize(), "Expected value to now be set");

          LOG.trace("Server sender opened");
          serverSender.open();

          serverAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.openHandler(x -> {
          LOG.trace("Client connection opened");
          final ProtonLink<?> receiver = connection.createReceiver("some-address");

          context.assertNull(receiver.getMaxMessageSize(), "Expected no value to be set");
          receiver.setMaxMessageSize(clientMaxMsgSize);
          context.assertEquals(clientMaxMsgSize, receiver.getMaxMessageSize(), "Expected value to now be set");

          receiver.openHandler(y -> {
            LOG.trace("Client link opened");
            context.assertEquals(serverMaxMsgSize, receiver.getRemoteMaxMessageSize(),
                "unexpected remote max message size at client");

            clientAsync.complete();
          });
          receiver.open();

        }).open();
      });

      serverAsync.awaitSuccess();
      clientAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testLinkPropertiesAndCapabilities(TestContext context) throws Exception {
    server.close();
    Async serverAsync = context.async();
    Async clientAsync = context.async();

    Map<Symbol,Object> clientProperties = Collections.singletonMap(Symbol.valueOf("client"), true);
    Map<Symbol,Object> serverProperties = Collections.singletonMap(Symbol.valueOf("server"), true);
    Symbol cap1 = Symbol.valueOf("1");
    Symbol cap2 = Symbol.valueOf("1");
    Symbol cap3 = Symbol.valueOf("1");
    Symbol cap4 = Symbol.valueOf("1");
    Symbol[] clientDesired = { cap1, cap2 };
    Symbol[] clientOffered = { cap3 };
    Symbol[] serverDesried = { cap3, cap4 };
    Symbol[] serverOffered = { cap2 };

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });
        serverConnection.sessionOpenHandler(session -> {
          session.open();
        });

        serverConnection.senderOpenHandler(serverSender -> {
          context.assertEquals(clientProperties, serverSender.getRemoteProperties());
          context.assertTrue(Arrays.equals(clientDesired, serverSender.getRemoteDesiredCapabilities()));
          context.assertTrue(Arrays.equals(clientOffered, serverSender.getRemoteOfferedCapabilities()));


          LOG.trace("Server sender opened");
          serverSender.setProperties(serverProperties);
          serverSender.setDesiredCapabilities(serverDesried);
          serverSender.setOfferedCapabilities(serverOffered);
          serverSender.open();

          serverAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.openHandler(x -> {
          LOG.trace("Client connection opened");
          final ProtonLink<?> receiver = connection.createReceiver("some-address");

          receiver.setProperties(clientProperties);
          receiver.setDesiredCapabilities(clientDesired);
          receiver.setOfferedCapabilities(clientOffered);
          receiver.openHandler(y -> {
            LOG.trace("Client link opened");
            context.assertEquals(serverProperties, receiver.getRemoteProperties(), "Unexpected value for link properties");
            context.assertTrue(Arrays.equals(serverDesried, receiver.getRemoteDesiredCapabilities()));
            context.assertTrue(Arrays.equals(serverOffered, receiver.getRemoteOfferedCapabilities()));


            clientAsync.complete();
          });
          receiver.open();

        }).open();
      });

      serverAsync.awaitSuccess();
      clientAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testFailedMessageDecoding(TestContext context) throws Exception {
    server.close();

    Async serverAsync = context.async();
    Async clientAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });
        serverConnection.sessionOpenHandler(session -> {
          session.open();
        });

        serverConnection.senderOpenHandler(serverSender -> {
          serverSender.open();

          AtomicInteger count = new AtomicInteger();
          serverSender.sendQueueDrainHandler(s -> {
            int msg = count.incrementAndGet();

            switch (msg) {
              case 1:
                context.assertEquals(1000, s.getCredit(), "Unexpected initial credit level when send handler fired for round 1");
                serverSender.send(message(String.valueOf(msg)), del -> {
                  context.assertTrue(del.getRemoteState() instanceof Accepted, "Unexpected state for delivery 1 after update");
                });
                break;
              case 2:
                MessageImpl invalidEncodingMessage = Mockito.mock(MessageImpl.class);
                Mockito.when(invalidEncodingMessage.encode(Mockito.any(WritableBuffer.class)))
                .then(i -> {
                  WritableBuffer buffer = i.getArgument(0);
                  byte[] invalid = new byte[5];
                  buffer.put(invalid, 0, invalid.length);
                  return invalid.length;
                });

                serverSender.send(invalidEncodingMessage, del -> {
                  DeliveryState state = del.getRemoteState();
                  context.assertTrue(state instanceof Modified, "Unexpected state for delivery 2 after update");
                  context.assertTrue(((Modified)state).getDeliveryFailed(), "Expected true");
                  context.assertTrue(((Modified)state).getUndeliverableHere(), "Expected true");
                });
                break;
              case 3:
                serverSender.send(message(String.valueOf(msg)), del -> {
                  context.assertTrue(del.getRemoteState() instanceof Accepted, "Unexpected state for delivery 3 after update");
                  // We've sent 3 messages, consumer should receive 2 and fail to decode 1.
                  // Verify credit is fully replenished to initial 1000 in the end.
                  vertx.setTimer(500, x -> {
                    context.assertEquals(1000, s.getCredit(), "Unexpected credit level after messages processed");
                    serverAsync.complete();
                  });
                });
                break;
            }
          });
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();
        AtomicInteger counter = new AtomicInteger(0);
        ProtonReceiver receiver = connection.createReceiver("address");

        receiver.handler((d, m) -> {
          int count = counter.incrementAndGet();
          switch (count) {
            case 1:
              validateMessage(context, 1, "1", m);
              break;
            case 2:
              validateMessage(context, 3, "3", m);
              clientAsync.complete();
              break;
          }
        }).open();
      });

      serverAsync.awaitSuccess();
      clientAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  private ProtonServer createServer(Handler<ProtonConnection> serverConnHandler) throws InterruptedException,
                                                                                 ExecutionException {
    ProtonServer server = ProtonServer.create(vertx);

    server.connectHandler(serverConnHandler);

    FutureHandler<ProtonServer, AsyncResult<ProtonServer>> handler = FutureHandler.asyncResult();
    server.listen(0, handler);
    handler.get();

    return server;
  }

  private void validateMessage(TestContext context, int count, Object expected, Message msg) {
    Object actual = getMessageBody(context, msg);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Got msg " + count + ", body: " + actual);
    }

    context.assertEquals(expected, actual, "Unexpected message body");
  }

  private Object getMessageBody(TestContext context, Message msg) {
    Section body = msg.getBody();

    context.assertNotNull(body);
    context.assertTrue(body instanceof AmqpValue);

    return ((AmqpValue) body).getValue();
  }
}
