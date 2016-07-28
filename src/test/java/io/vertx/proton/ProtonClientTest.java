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
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.impl.ProtonMetaDataSupportImpl;
import io.vertx.proton.impl.ProtonServerImpl;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.vertx.proton.ProtonHelper.message;
import static io.vertx.proton.ProtonHelper.tag;

@RunWith(VertxUnitRunner.class)
public class ProtonClientTest extends MockServerTestBase {

  private static Logger LOG = LoggerFactory.getLogger(ProtonClientTest.class);

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
        async.complete();
      });

      // Send a request to the server for him to disconnect us
      ProtonSender sender = connection.createSender(null).open();
      sender.send(tag(""), message("command", "disconnect"));
    });
  }

  @Test(timeout = 20000)
  public void testLocalDisconnectHandling(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      context.assertFalse(connection.isDisconnected());
      connection.disconnectHandler(x -> {
        context.assertTrue(connection.isDisconnected());
        async.complete();
      });
      // We will force the disconnection to the server
      connection.disconnect();
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

      connection.createSender(MockServer.Addresses.echo.toString()).open().send(tag(""), message("echo", data));

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
      Message messageWithNoAddress = Proton.message();
      try {
        sender.send(tag("t1"), messageWithNoAddress);
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
      Message messageWithNoAddress = Proton.message();
      sender.send(tag("t1"), messageWithNoAddress);
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
          sender.send(tag("tag"), message("ignored", "content"));
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
        LOG.trace("Server got msg: {0}", getMessageBody(context, msg));
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
        Future<ProtonSession> sessionFuture = Future.<ProtonSession> future();
        // Expect a session to open, when the sender is created by the client
        serverConnection.sessionOpenHandler(serverSession -> {
          LOG.trace("Server session open");
          serverSession.open();
          sessionFuture.complete(serverSession);
        });
        // Expect a receiver link, then close the session after opening it.
        serverConnection.receiverOpenHandler(serverReceiver -> {
          LOG.trace("Server receiver open");
          serverReceiver.open();

          context.assertTrue(sessionFuture.succeeded(), "Session future not [yet] succeeded");
          LOG.trace("Server session close");
          ProtonSession s = sessionFuture.result();
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
    LOG.trace("Got msg {0}, body: {1}", count, actual);

    context.assertEquals(expected, actual, "Unexpected message body");
  }

  private Object getMessageBody(TestContext context, Message msg) {
    Section body = msg.getBody();

    context.assertNotNull(body);
    context.assertTrue(body instanceof AmqpValue);

    return ((AmqpValue) body).getValue();
  }
}
