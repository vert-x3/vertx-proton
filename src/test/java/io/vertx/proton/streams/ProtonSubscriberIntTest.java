/*
* Copyright 2018 the original author or authors.
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
package io.vertx.proton.streams;

import static io.vertx.proton.ProtonHelper.message;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.FutureHandler;
import io.vertx.proton.MockServerTestBase;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;

@RunWith(VertxUnitRunner.class)
public class ProtonSubscriberIntTest extends MockServerTestBase {

  private static Logger LOG = LoggerFactory.getLogger(ProtonSubscriberIntTest.class);

  //TODO: test retrieve delivery state from tracker [other than accept]

  @Test(timeout = 20000)
  public void testCreateUseAndCancelSubscriber(TestContext context) throws Exception {
    server.close();

    final Async serverLinkOpenAsync = context.async();
    final Async serverReceivedMessageAsync = context.async();
    final Async serverLinkCloseAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.receiverOpenHandler(serverReceiver -> {
          LOG.trace("Server receiver opened");

          serverReceiver.handler((delivery, msg) -> {
            // We got the message that was sent, complete the test
            if (LOG.isTraceEnabled()) {
              LOG.trace("Server got msg: " + getMessageBody(context, msg));
            }
            validateMessage(context, 1, "1", msg);
            serverReceivedMessageAsync.complete();
          });

          serverReceiver.closeHandler(x -> {
            serverReceiver.close();
            serverLinkCloseAsync.complete();
          });

          // Set the local terminus details [naively]
          serverReceiver.setTarget(serverReceiver.getRemoteTarget().copy());

          serverReceiver.open();

          serverLinkOpenAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        ProtonSubscriber<Tracker> subscriber = ProtonStreams.createTrackerProducer(connection,"myAddress");

        Tracker tracker = Tracker.create(message("1"), t -> {
          context.assertTrue(t.isAccepted(), "msg should be accepted");
          context.assertTrue(t.isRemotelySettled(), "msg should be remotely settled");
        });

        Publisher<Tracker> producer = Flowable.just(tracker);
        producer.subscribe(subscriber);
      });

      serverLinkOpenAsync.awaitSuccess();
      serverReceivedMessageAsync.awaitSuccess();
      serverLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testSubCancelledOnLinkClose(TestContext context) throws Exception {
    server.close();

    final Async serverLinkOpenAsync = context.async();
    final Async clientLinkCloseAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.receiverOpenHandler(serverReceiver -> {
          LOG.trace("Server receiver opened");
          // Set the local terminus details [naively]
          serverReceiver.setTarget(serverReceiver.getRemoteTarget().copy());

          serverReceiver.open();

          serverLinkOpenAsync.complete();

          serverReceiver.close();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        ProtonSubscriber<Tracker> subscriber = ProtonStreams.createTrackerProducer(connection,"myAddress");

        // Create a Publisher that doesn't produce elements, but indicates when its subscription is cancelled.
        Publisher<Tracker> producer = Flowable.<Tracker>never()
                                              .doOnCancel(() -> {
                                                LOG.trace("Cancelled!");
                                                clientLinkCloseAsync.complete();
                                              });
        producer.subscribe(subscriber);
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testSubCancelledOnConnectionEnd(TestContext context) throws Exception {
    server.close();

    final Async serverLinkOpenAsync = context.async();
    final Async subCancelled = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.receiverOpenHandler(serverReceiver -> {
          LOG.trace("Server receiver opened");
          // Set the local terminus details [naively]
          serverReceiver.setTarget(serverReceiver.getRemoteTarget().copy());

          serverReceiver.open();

          serverLinkOpenAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        ProtonSubscriber<Tracker> subscriber = ProtonStreams.createTrackerProducer(connection,"myAddress");

        // Create a Publisher that doesn't produce elements, but indicates when its subscription is cancelled.
        Publisher<Tracker> producer = Flowable.<Tracker>never()
                                              .doOnCancel(() -> {
                                                LOG.trace("Cancelled!");
                                                subCancelled.complete();
                                              });
        producer.subscribe(subscriber);
      });

      serverLinkOpenAsync.awaitSuccess();
      context.assertFalse(subCancelled.isCompleted());
      protonServer.close();
      protonServer = null;
      subCancelled.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testConfigureProducerLinkName(TestContext context) throws Exception {
    server.close();

    final Async serverLinkOpenAsync = context.async();
    final Async serverReceivedMessageAsync = context.async();
    final Async serverLinkCloseAsync = context.async();

    final String linkName = "testConfigureProducerLinkName";

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.receiverOpenHandler(serverReceiver -> {
          LOG.trace("Server receiver opened");

          // Verify the link details used were as expected
          context.assertEquals(linkName, serverReceiver.getName(), "unexpected link name");

          serverReceiver.handler((delivery, msg) -> {
            // We got the message that was sent, complete the test
            if (LOG.isTraceEnabled()) {
              LOG.trace("Server got msg: " + getMessageBody(context, msg));
            }
            validateMessage(context, 1, "1", msg);
            serverReceivedMessageAsync.complete();
          });

          serverReceiver.closeHandler(x -> {
            serverReceiver.close();
            serverLinkCloseAsync.complete();
          });

          // Set the local terminus details [naively]
          serverReceiver.setTarget(serverReceiver.getRemoteTarget().copy());

          serverReceiver.open();

          serverLinkOpenAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        // Create subscriber with given link name
        ProtonSubscriberOptions options = new ProtonSubscriberOptions().setLinkName(linkName);

        ProtonSubscriber<Tracker> subscriber = ProtonStreams.createTrackerProducer(connection,"myAddress", options);

        Tracker envelope = Tracker.create(message("1"));
        Publisher<Tracker> producer = Flowable.just(envelope);
        producer.subscribe(subscriber);
      });

      serverLinkOpenAsync.awaitSuccess();
      serverReceivedMessageAsync.awaitSuccess();
      serverLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testCreateAnonymousRelaySubscriber(TestContext context) throws Exception {
    server.close();

    final Async serverLinkOpenAsync = context.async();
    final Async serverReceivedMessageAsync = context.async();
    final Async serverLinkCloseAsync = context.async();

    final String address1 = "testCreateAnonymousRelaySubscriber1";
    final String address2 = "testCreateAnonymousRelaySubscriber2";

    final AtomicInteger msgCounter = new AtomicInteger(0);
    final AtomicInteger acceptedMsgCounter = new AtomicInteger(0);

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.receiverOpenHandler(serverReceiver -> {
          LOG.trace("Server receiver opened");

          context.assertFalse(serverLinkOpenAsync.isCompleted(), "should only be one link opened");
          context.assertNull(serverReceiver.getRemoteTarget().getAddress(), "link target should have null address for anonymous relay");

          serverReceiver.handler((delivery, msg) -> {
            int count = msgCounter.incrementAndGet();
            if (LOG.isTraceEnabled()) {
              LOG.trace("Server got msg: " + getMessageBody(context, msg));
            }
            switch (count) {
              case 1: {
                validateMessage(context, 1, "1", msg);
                context.assertEquals(address1, msg.getAddress(), "Unexpected message1 'to' address");
                break;
              }
              case 2: {
                validateMessage(context, 2, "2", msg);
                context.assertEquals(address2, msg.getAddress(), "Unexpected message2 'to' address");

                //Complete the test
                serverReceivedMessageAsync.complete();
                break;
              }
            }
          });

          serverReceiver.closeHandler(x -> {
            serverReceiver.close();
            serverLinkCloseAsync.complete();
          });

          // Set the local terminus details [naively]
          serverReceiver.setTarget(serverReceiver.getRemoteTarget().copy());

          serverReceiver.open();

          serverLinkOpenAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        // Create subscriber without an address, to send to the anonymous relay
        ProtonSubscriber<Tracker> subscriber = ProtonStreams.createTrackerProducer(connection, null);

        Tracker envelope1 = Tracker.create(message(address1, "1"), env1 -> {
          context.assertTrue(env1.isAccepted(), "msg1 should be accepted");
          context.assertTrue(env1.isRemotelySettled(), "msg1 should be remotely settled");
          context.assertTrue(acceptedMsgCounter.compareAndSet(0, 1), "unexpected acceptedMsgCounter:" + acceptedMsgCounter);
        });
        Tracker envelope2 = Tracker.create(message(address2, "2"), env2 -> {
          context.assertTrue(env2.isAccepted(), "msg1 should be accepted");
          context.assertTrue(env2.isRemotelySettled(), "msg1 should be remotely settled");
          context.assertTrue(acceptedMsgCounter.compareAndSet(1, 2), "unexpected acceptedMsgCounter:" + acceptedMsgCounter);
        });
        Publisher<Tracker> producer = Flowable.just(envelope1, envelope2);
        producer.subscribe(subscriber);
      });

      serverLinkOpenAsync.awaitSuccess();
      serverReceivedMessageAsync.awaitSuccess();
      serverLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testAlternativeDispositionStates(TestContext context) throws Exception {
    server.close();

    final Async serverLinkOpenAsync = context.async();
    final Async serverReceivedMessageAsync = context.async();
    final Async serverLinkCloseAsync = context.async();
    int messageCount = 4;

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        AtomicInteger counter = new AtomicInteger(0);
        serverConnection.receiverOpenHandler(serverReceiver -> {
          LOG.trace("Server receiver opened");
          serverReceiver.setAutoAccept(false);

          serverReceiver.handler((delivery, msg) -> {
            int msgNum = counter.incrementAndGet();
            // We got the message that was sent, validate it
            if (LOG.isTraceEnabled()) {
              LOG.trace("Server got msg: " + getMessageBody(context, msg));
            }
            validateMessage(context, msgNum, String.valueOf(msgNum), msg);

            switch (msgNum) {
              case 1: {
                delivery.disposition(Accepted.getInstance(), true);
                break;
              }
              case 2: {
                delivery.disposition(Released.getInstance(), true);
                break;
              }
              case 3: {
                delivery.disposition(new Rejected(), true);
                break;
              }
              case 4: {
                delivery.disposition(new Modified(), true);
                break;
              }
            }

            if(msgNum == messageCount) {
              // Received all the messages
              serverReceivedMessageAsync.complete();
            }
          });

          serverReceiver.closeHandler(x -> {
            serverReceiver.close();
            serverLinkCloseAsync.complete();
          });

          // Set the local terminus details [naively]
          serverReceiver.setTarget(serverReceiver.getRemoteTarget().copy());

          serverReceiver.open();

          serverLinkOpenAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        ProtonSubscriber<Tracker> subscriber = ProtonStreams.createTrackerProducer(connection,"myAddress");

        Tracker tracker1 = Tracker.create(message("1"), t -> {
          context.assertTrue(t.isAccepted(), "msg should be accepted");
          context.assertTrue(t.isRemotelySettled(), "msg should be remotely settled");
        });

        Tracker tracker2 = Tracker.create(message("2"), t -> {
          context.assertFalse(t.isAccepted(), "msg should not be accepted");
          context.assertTrue(t.getRemoteState() instanceof Released, "unexpected remote state: "+ t.getRemoteState());
          context.assertTrue(t.isRemotelySettled(), "msg should be remotely settled");
        });

        Tracker tracker3 = Tracker.create(message("3"), t -> {
          context.assertFalse(t.isAccepted(), "msg should not be accepted");
          context.assertTrue(t.getRemoteState() instanceof Rejected, "unexpected remote state: "+ t.getRemoteState());
          context.assertTrue(t.isRemotelySettled(), "msg should be remotely settled");
        });

        Tracker tracker4 = Tracker.create(message("4"), t -> {
          context.assertFalse(t.isAccepted(), "msg should not be accepted");
          context.assertTrue(t.getRemoteState() instanceof Modified, "unexpected remote state: "+ t.getRemoteState());
          context.assertTrue(t.isRemotelySettled(), "msg should be remotely settled");
        });

        Publisher<Tracker> producer = Flowable.just(tracker1, tracker2, tracker3, tracker4);
        producer.subscribe(subscriber);
      });

      serverLinkOpenAsync.awaitSuccess();
      serverReceivedMessageAsync.awaitSuccess();
      serverLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testCreateUsingCustomTarget(TestContext context) throws Exception {
    server.close();

    final Async serverLinkOpenAsync = context.async();
    final Async serverReceivedMessageAsync = context.async();
    final Async serverLinkCloseAsync = context.async();

    final String address = "testCreateUsingCustomTarget";

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.receiverOpenHandler(serverReceiver -> {
          serverReceiver.handler((delivery, msg) -> {
            // We got the message that was sent, complete the test
            if (LOG.isTraceEnabled()) {
              LOG.trace("Server got msg: " + getMessageBody(context, msg));
            }
            validateMessage(context, 1, "1", msg);
            serverReceivedMessageAsync.complete();
          });

          // Verify the remote terminus details used were as expected
          context.assertNotNull(serverReceiver.getRemoteTarget(), "target should not be null");
          org.apache.qpid.proton.amqp.messaging.Target remoteTarget = (org.apache.qpid.proton.amqp.messaging.Target) serverReceiver.getRemoteTarget();
          context.assertEquals(address, remoteTarget.getAddress(), "unexpected target address");

          Symbol[] capabilities = remoteTarget.getCapabilities();
          context.assertTrue(Arrays.equals(new Symbol[] {Symbol.valueOf("custom")}, capabilities), "Unexpected capabilities: " + Arrays.toString(capabilities));

          // Set the local terminus details
          serverReceiver.setTarget(remoteTarget.copy());

          serverReceiver.closeHandler(x -> {
            serverReceiver.close();
            serverLinkCloseAsync.complete();
          });

          LOG.trace("Server receiver opened");
          serverReceiver.open();

          serverLinkOpenAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        // Create subscriber with custom target configured to be dynamic
        ProtonSubscriber<Tracker> subscriber = ProtonStreams.createTrackerProducer(connection, address);
        org.apache.qpid.proton.amqp.messaging.Target target = (org.apache.qpid.proton.amqp.messaging.Target) subscriber.getTarget();
        target.setCapabilities(new Symbol[] {Symbol.valueOf("custom")});

        Tracker envelope = Tracker.create(message("1"));
        Publisher<Tracker> publisher = Flowable.just(envelope);
        publisher.subscribe(subscriber);
      });

      serverLinkOpenAsync.awaitSuccess();
      serverReceivedMessageAsync.awaitSuccess();
      serverLinkCloseAsync.awaitSuccess();
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
