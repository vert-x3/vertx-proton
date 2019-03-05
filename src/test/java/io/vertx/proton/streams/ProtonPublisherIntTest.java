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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.amqp.messaging.TerminusExpiryPolicy;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.FutureHandler;
import io.vertx.proton.MockServerTestBase;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.streams.Delivery;
import io.vertx.proton.streams.ProtonPublisher;
import io.vertx.proton.streams.ProtonStreams;

@RunWith(VertxUnitRunner.class)
public class ProtonPublisherIntTest extends MockServerTestBase {

  private static Logger LOG = LoggerFactory.getLogger(ProtonPublisherIntTest.class);

  @Test(timeout = 20000)
  public void testCreateCancelSubscription(TestContext context) throws Exception {
    server.close();

    final Async clientLinkOpenAsync = context.async();
    final Async serverLinkOpenAsync = context.async();
    final Async serverLinkCloseAsync = context.async();
    final Async clientLinkCloseAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          serverSender.closeHandler(res -> {
            serverLinkCloseAsync.complete();
            serverSender.close();
          });

          // Buffer a single message to send when credit arrives
          serverSender.send(message(String.valueOf(1)));

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

        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"myAddress");

        publisher.subscribe(new Subscriber<Delivery>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            clientLinkOpenAsync.complete();
            sub = s;
            s.request(5);
          }

          @Override
          public void onNext(Delivery d) {
            validateMessage(context, 1, String.valueOf(1), d.message());
            sub.cancel();
          }

          @Override
          public void onError(Throwable t) {
            if(!clientLinkCloseAsync.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            clientLinkCloseAsync.complete();
          }
        });
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkOpenAsync.awaitSuccess();

      serverLinkCloseAsync.awaitSuccess();
      clientLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testSubscriberErrorOnLinkCLose(TestContext context) throws Exception {
    server.close();

    final Async clientLinkOpenAsync = context.async();
    final Async serverLinkOpenAsync = context.async();
    final Async serverLinkCloseAsync = context.async();
    final Async clientLinkCloseAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          serverSender.closeHandler(res -> {
            serverLinkCloseAsync.complete();
          });

          LOG.trace("Server sender opened");
          serverSender.open();
          serverLinkOpenAsync.complete();

          serverSender.close();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"myAddress");

        publisher.subscribe(new Subscriber<Delivery>() {
          @Override
          public void onSubscribe(Subscription s) {
            clientLinkOpenAsync.complete();
          }

          @Override
          public void onNext(Delivery e) {
            context.fail("onNext called");
          }

          @Override
          public void onError(Throwable t) {
            clientLinkCloseAsync.complete();
          }

          @Override
          public void onComplete() {
            context.fail("onComplete called");
          }
        });
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkOpenAsync.awaitSuccess();
      clientLinkCloseAsync.awaitSuccess();
      serverLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testSubscriberErrorOnConnectionEnd(TestContext context) throws Exception {
    server.close();

    final Async clientLinkOpenAsync = context.async();
    final Async serverLinkOpenAsync = context.async();
    final Async subErroredAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
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

        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"myAddress");

        publisher.subscribe(new Subscriber<Delivery>() {
          @Override
          public void onSubscribe(Subscription s) {
            clientLinkOpenAsync.complete();
          }

          @Override
          public void onNext(Delivery e) {
            context.fail("onNext called");
          }

          @Override
          public void onError(Throwable t) {
            LOG.trace("onError called");
            subErroredAsync.complete();
          }

          @Override
          public void onComplete() {
            LOG.trace("onComplete called");
            context.fail("onComplete called");
          }
        });
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkOpenAsync.awaitSuccess();
      context.assertFalse(subErroredAsync.isCompleted());
      protonServer.close();
      protonServer = null;
      subErroredAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testConfigureSubscriptionDynamic(TestContext context) throws Exception {
    server.close();

    final Async clientLinkOpenAsync = context.async();
    final Async serverLinkOpenAsync = context.async();
    final Async clientLinkCloseAsync = context.async();

    final String dynamicAddress = "testConfigureSubscriptionDynamic:" + UUID.randomUUID();

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
          context.assertTrue(remoteSource.getDynamic(), "expected dynamic source");
          context.assertNull(remoteSource.getAddress(), "expected no source address");

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

        // Create publisher with dynamic option
        ProtonPublisherOptions options = new ProtonPublisherOptions().setDynamic(true);
        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection, null, options);

        publisher.subscribe(new Subscriber<Delivery>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            // Verify the remote address detail
            context.assertEquals(dynamicAddress, publisher.getRemoteAddress(), "unexpected remote address");

            // Grab and verify the source details
            org.apache.qpid.proton.amqp.messaging.Source remoteSource = (org.apache.qpid.proton.amqp.messaging.Source) publisher.getRemoteSource();
            context.assertTrue(remoteSource.getDynamic(), "expected dynamic source");
            context.assertEquals(dynamicAddress, remoteSource.getAddress(), "unexpected source address");

            clientLinkOpenAsync.complete();

            sub = s;
            s.request(1);
            sub.cancel();
          }

          @Override
          public void onNext(Delivery e) {
          }

          @Override
          public void onError(Throwable t) {
            if(!clientLinkCloseAsync.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            clientLinkCloseAsync.complete();
          }
        });
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkOpenAsync.awaitSuccess();

      clientLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testDelayedInitialRequest(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      AtomicInteger counter = new AtomicInteger(0);
      AtomicBoolean initialCreditGranted = new AtomicBoolean();
      AtomicBoolean additionalCreditGranted = new AtomicBoolean();
      final int delay = 250;
      final long startTime = System.currentTimeMillis();

      ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"two_messages");

      publisher.subscribe(new Subscriber<Delivery>() {
        Subscription sub = null;

        @Override
        public void onSubscribe(Subscription s) {
          vertx.setTimer(delay, x -> {
            LOG.trace("Flowing initial credit");
            initialCreditGranted.set(true);
            sub = s;
            sub.request(1);
          });
        }

        @Override
        public void onNext(Delivery d) {
          int count = counter.incrementAndGet();
          switch (count) {
            case 1: {
              validateMessage(context, count, String.valueOf(count), d.message());

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
                sub.request(1);
              });
              break;
            }
            case 2: {
              validateMessage(context, count, String.valueOf(count), d.message());
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
        }

        @Override
        public void onError(Throwable t) {
          if(!async.isCompleted()) {
            context.fail("onError called");
          }
        }

        @Override
        public void onComplete() {
          context.fail("onComplete called");
        }
      });
    });
  }

  @Test(timeout = 20000)
  public void testImmediateInitialRequest(TestContext context) {
    Async async = context.async();
    connect(context, connection -> {
      connection.open();
      AtomicInteger counter = new AtomicInteger(0);
      AtomicBoolean creditGranted = new AtomicBoolean();

      ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"five_messages");

      publisher.subscribe(new Subscriber<Delivery>() {
        Subscription sub = null;

        @Override
        public void onSubscribe(Subscription s) {
          // Explicitly grant initial credit of 4, onNext will grant more later.
          LOG.trace("Flowing initial credit");
          sub = s;
          sub.request(4);
        }

        @Override
        public void onNext(Delivery d) {
          int count = counter.incrementAndGet();
          switch (count) {
            case 1: // Fall-through
            case 2: // Fall-through
            case 3: {
              validateMessage(context, count, String.valueOf(count), d.message());
              break;
            }
            case 4: {
              validateMessage(context, count, String.valueOf(count),  d.message());

              // We only issued 4 credits, so we should not get
              // more messages until more credit is flowed, use
              // the callback for this msg to do that
              vertx.setTimer(1000, x -> {
                LOG.trace("Flowing more credit");
                creditGranted.set(true);
                sub.request(1);
              });

              // Check that we haven't processed any more messages before then
              vertx.setTimer(500, x -> {
                LOG.trace("Checking msg 5 not received yet");
                context.assertEquals(4, counter.get());
              });
              break;
            }
            case 5: {
              validateMessage(context, count, String.valueOf(count),  d.message());
              context.assertTrue(creditGranted.get(),
                  "Additional credit not yet granted, so we" + " should not have received message 5 yet!");

              // Got the last message, lets finish the test.
              LOG.trace("Got msg 5, completing async");
              async.complete();
              connection.disconnect();
              break;
            }
          }
        }

        @Override
        public void onError(Throwable t) {
          if(!async.isCompleted()) {
            context.fail("onError called");
          }
        }

        @Override
        public void onComplete() {
          context.fail("onComplete called");
        }
      });
    });
  }

  @Test(timeout = 20000)
  public void testMaxOutstandingCredit1(TestContext context) throws Exception {
    server.close();

    final int maxOutstanding = 1000;
    final int amount1 = 15;
    final int amount2 = 1100;

    final int totalRequests = amount1 + amount2;

    final Async receivedCreditAsync = context.async();
    final Async verifiedCreditAsync = context.async();

    final Async receivedCreditAsync2 = context.async();
    final Async receivedMessages = context.async();

    List<Integer> credits = new ArrayList<>();
    AtomicInteger counter = new AtomicInteger();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          LOG.trace("Server sender opened");
          serverSender.open();

          AtomicInteger msgNum = new AtomicInteger();
          serverSender.sendQueueDrainHandler(s -> {
            int credit = serverSender.getCredit();

            LOG.trace("Server credit: " + credit);
            credits.add(credit);

            if(credit > maxOutstanding) {
              context.fail("Received unexpected amount of credit: "+ credit);
            } else if(credit == maxOutstanding) {
              LOG.trace("Server reached max outstanding: " + credit);
              receivedCreditAsync.complete();
              verifiedCreditAsync.await();

              while(!serverSender.sendQueueFull()) {
                serverSender.send(message(String.valueOf(msgNum.incrementAndGet())));
              }

              return;
            } else if(receivedCreditAsync.isCompleted()) {
              LOG.trace("Server received topup credit, post-max-outstanding: " + credit);
              receivedCreditAsync2.complete();
              while(!serverSender.sendQueueFull()) {
                serverSender.send(message(String.valueOf(msgNum.incrementAndGet())));
              }
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

        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"myAddress");

        publisher.subscribe(new Subscriber<Delivery>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            LOG.trace("Flowing initial credit");
            sub = s;
            sub.request(amount1);
            vertx.setTimer(250, x -> {
              LOG.trace("Granting additional credit");
              sub.request(amount2);
            });
          }

          @Override
          public void onNext(Delivery d) {
            int count = counter.incrementAndGet();
            validateMessage(context, count, String.valueOf(count), d.message());

            if(count >= totalRequests) {
              receivedMessages.complete();
            }
          }

          @Override
          public void onError(Throwable t) {
            if(!receivedMessages.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            context.fail("onComplete called");
          }
        });
      });

      receivedCreditAsync.awaitSuccess();
      // Verify the requests were all received as expected
      context.assertEquals(credits.size(), 2, "Unexpected credits count");
      context.assertEquals(credits.get(0), amount1, "Unexpected credit 1");
      context.assertEquals(credits.get(1), maxOutstanding, "Unexpected credit 2");
      verifiedCreditAsync.complete();

      receivedCreditAsync2.awaitSuccess();
      // Verify the requests were all received as expected
      context.assertEquals(credits.size(), 3, "Unexpected credits count");
      context.assertEquals(credits.get(2), amount1 + amount2 - maxOutstanding, "Unexpected credit 3");

      receivedMessages.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testMaxOutstandingCredit2(TestContext context) throws Exception {
    server.close();

    final int maxOutstandingCredit = 20;
    final int totalRequests = 107;

    final Async receivedMessagesAsync = context.async();

    AtomicInteger receivedCredits = new AtomicInteger();
    AtomicInteger maxCredits = new AtomicInteger();
    AtomicInteger counter = new AtomicInteger();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          LOG.trace("Server sender opened");
          serverSender.open();

          AtomicInteger msgNum = new AtomicInteger();
          serverSender.sendQueueDrainHandler(s -> {
            int credit = serverSender.getCredit();

            LOG.trace("Server credit: " + credit);
            int max = maxCredits.get();
            if(credit > max) {
              maxCredits.compareAndSet(max, credit);
            }

            while(!serverSender.sendQueueFull()) {
              serverSender.send(message(String.valueOf(msgNum.incrementAndGet())));
            }

            // Simple count, only works because of the thread model and using all credits as received
            receivedCredits.addAndGet(credit);
          });
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();

        connection.open();

        // Create publisher with set maxOutstanding
        ProtonPublisherOptions options = new ProtonPublisherOptions().setMaxOutstandingCredit(maxOutstandingCredit);
        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection, "myAddress", options);

        publisher.subscribe(new Subscriber<Delivery>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            LOG.trace("Flowing initial credit");
            sub = s;
            sub.request(totalRequests);
          }

          @Override
          public void onNext(Delivery d) {
            int count = counter.incrementAndGet();
            validateMessage(context, count, String.valueOf(count), d.message());

            if(count >= totalRequests) {
              receivedMessagesAsync.complete();
            }
          }

          @Override
          public void onError(Throwable t) {
            if(!receivedMessagesAsync.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            context.fail("onComplete called");
          }
        });
      });

      // Verify the max outstanding was honoured and messages were all received as expected
      receivedMessagesAsync.awaitSuccess();
      context.assertEquals(maxCredits.get(), maxOutstandingCredit, "Unexpected max credit");
      context.assertEquals(receivedCredits.get(), totalRequests, "Unexpected total credits received");
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testAutoAccept(TestContext context) throws Exception {
    server.close();

    final Async receivedMessagesAsync = context.async();
    final Async ackedMessagesAsync = context.async();
    int messageCount = 5;
    List<Integer> accepted = Collections.synchronizedList(new ArrayList<>());
    AtomicInteger msgNum = new AtomicInteger(1);

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          LOG.trace("Server sender opened");
          serverSender.open();

          serverSender.sendQueueDrainHandler(s -> {
            for(int i = msgNum.get(); i <= messageCount; i = msgNum.incrementAndGet()){
              int j= i;
              serverSender.send(message(String.valueOf(i)), del -> {
                LOG.trace("Server received disposition for msg: " + j);
                if(del.getRemoteState() instanceof Accepted) {
                  accepted.add(j);

                  if(accepted.size() == messageCount) {
                    ackedMessagesAsync.complete();
                  }
                } else {
                  context.fail("Expected message to be accepted");
                }
              });
            }
          });
        });
      });

      // ===== Client Handling =====

      AtomicInteger counter = new AtomicInteger(0);
      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();

        // Create consumer stream of Message, which auto-accepts after onNext
        ProtonPublisher<Message> publisher = ProtonStreams.createConsumer(connection,"myAddress");
        publisher.subscribe(new Subscriber<Message>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            sub = s;
            LOG.trace("Flowing initial credit");
            sub.request(messageCount);
          }

          @Override
          public void onNext(Message m) {
            int count = counter.incrementAndGet();
            validateMessage(context, count, String.valueOf(count), m);
            if(count == messageCount) {
                LOG.trace("Got all messages, completing async");
                receivedMessagesAsync.complete();
            }
          }

          @Override
          public void onError(Throwable t) {
            if(!receivedMessagesAsync.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            context.fail("onComplete called");
          }
        });

        connection.open();
      });

      receivedMessagesAsync.awaitSuccess();
      ackedMessagesAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }

    // Verify the messages were all accepted
    context.assertEquals(accepted.size(), messageCount, "Unexpected accepted count");
    for(int i=1; i <=messageCount; i++) {
      // Verify each accepted number, establish correct order etc.
      context.assertEquals(accepted.remove(0), i, "Unexpected msgNum");
    }
  }

  @Test(timeout = 20000)
  public void testManualAccept(TestContext context) throws Exception {
    server.close();

    final Async receivedMessagesAsync = context.async();
    final Async ackedMessagesAsync = context.async();
    int messageCount = 5;
    List<Integer> accepted = Collections.synchronizedList(new ArrayList<>());
    List<Delivery> deliveries = Collections.synchronizedList(new ArrayList<>());

    AtomicInteger msgNum = new AtomicInteger(1);

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          LOG.trace("Server sender opened");
          serverSender.open();

          serverSender.sendQueueDrainHandler(s -> {
            for(int i = msgNum.get(); i <= messageCount; i = msgNum.incrementAndGet()){
              int j= i;
              serverSender.send(message(String.valueOf(i)), del -> {
                LOG.trace("Server received disposition for msg: " + j);
                if(del.getRemoteState() instanceof Accepted) {
                  accepted.add(j);

                  if(accepted.size() == messageCount) {
                    ackedMessagesAsync.complete();
                  }
                } else {
                  context.fail("Expected message to be accepted");
                }
              });
            }
          });
        });
      });

      // ===== Client Handling =====

      AtomicInteger counter = new AtomicInteger(0);
      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();

        // Create consumer stream of Delivery, which must be manually accepted
        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"myAddress");

        publisher.subscribe(new Subscriber<Delivery>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            sub = s;
            LOG.trace("Flowing initial credit");
            sub.request(messageCount);
          }

          @Override
          public void onNext(Delivery d) {
            int count = counter.incrementAndGet();
            deliveries.add(d);

            if(count == messageCount) {
                LOG.trace("Got all messages, completing async");
                receivedMessagesAsync.complete();
            }
          }

          @Override
          public void onError(Throwable t) {
            if(!receivedMessagesAsync.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            context.fail("onComplete called");
          }
        });

        connection.open();
      });

      receivedMessagesAsync.awaitSuccess();

      // Small delay to ensure there is a window for any
      // unexpected automatic accept to occur
      Thread.sleep(50);

      // Verify no messages accepted yet
      context.assertTrue(accepted.isEmpty(), "Unexpected accepted count");

      // Accept them all
      for(Delivery d: deliveries) {
        d.accept();
      }

      ackedMessagesAsync.awaitSuccess();

    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }

    // Verify the messages were all accepted
    context.assertEquals(accepted.size(), messageCount, "Unexpected accepted count");
    for(int i=1; i <=messageCount; i++) {
      // Verify each accepted number, establish correct order etc.
      context.assertEquals(accepted.remove(0), i, "Unexpected msgNum");
    }
  }

  @Test(timeout = 20000)
  public void testAlternativeDispositionStates(TestContext context) throws Exception {
    server.close();

    final Async receivedMessagesAsync = context.async();
    final Async ackedMessagesAsync = context.async();
    int messageCount = 4;
    List<DeliveryState> deliveryStates = Collections.synchronizedList(new ArrayList<>());
    List<Delivery> deliveries = Collections.synchronizedList(new ArrayList<>());

    AtomicInteger msgNum = new AtomicInteger(1);

    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          LOG.trace("Server sender opened");
          serverSender.open();

          serverSender.sendQueueDrainHandler(s -> {
            for(int i = msgNum.get(); i <= messageCount; i = msgNum.incrementAndGet()){
              int j= i;
              serverSender.send(message(String.valueOf(i)), del -> {
                LOG.trace("Server received disposition for msg: " + j);
                DeliveryState state = del.getRemoteState();
                context.assertNotNull(state, "Expected message to have a delivery state");
                deliveryStates.add(state);

                if(deliveryStates.size() == messageCount) {
                  ackedMessagesAsync.complete();
                }
              });
            }
          });
        });
      });

      // ===== Client Handling =====

      AtomicInteger counter = new AtomicInteger(0);
      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();

        // Create consumer stream of Delivery, which must be manually accepted
        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"myAddress");

        publisher.subscribe(new Subscriber<Delivery>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            sub = s;
            LOG.trace("Flowing initial credit");
            sub.request(messageCount);
          }

          @Override
          public void onNext(Delivery d) {
            int count = counter.incrementAndGet();
            deliveries.add(d);

            if(count == messageCount) {
                LOG.trace("Got all messages, completing async");
                receivedMessagesAsync.complete();
            }
          }

          @Override
          public void onError(Throwable t) {
            if(!receivedMessagesAsync.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            context.fail("onComplete called");
          }
        });

        connection.open();
      });

      receivedMessagesAsync.awaitSuccess();

      // Small delay to ensure there is a window for any
      // unexpected automatic accept to occur
      Thread.sleep(50);

      // Verify no messages acked yet
      context.assertTrue(deliveryStates.isEmpty(), "Unexpected acks count");

      // Ack them all
      deliveries.get(0).disposition(Accepted.getInstance(), true);
      deliveries.get(1).disposition(Released.getInstance(), true);
      deliveries.get(2).disposition(new Rejected(), true);
      deliveries.get(3).disposition(new Modified(), true);

      ackedMessagesAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }

    // Verify the messages were all acked as expected
    context.assertEquals(deliveryStates.size(), messageCount, "Unexpected state count: " + deliveryStates);
    context.assertTrue(deliveryStates.get(0) instanceof Accepted, "Unexpected state: " + deliveryStates.get(0));
    context.assertTrue(deliveryStates.get(1) instanceof Released, "Unexpected state: " + deliveryStates.get(1));
    context.assertTrue(deliveryStates.get(2) instanceof Rejected, "Unexpected state: " + deliveryStates.get(2));
    context.assertTrue(deliveryStates.get(3) instanceof Modified, "Unexpected state: " + deliveryStates.get(3));
  }

  @Test(timeout = 20000)
  public void testConfigureSubscriptionLinkName(TestContext context) throws Exception {
    doSubscriptionConfigTestImpl(context, false, "testConfigureSubscriptionLinkName", false, false);
  }

  @Test(timeout = 20000)
  public void testConfigureSubscriptionDurable(TestContext context) throws Exception {
    doSubscriptionConfigTestImpl(context, true, "testConfigureSubscriptionDurable", false, false);
  }

  @Test(timeout = 20000)
  public void testConfigureSubscriptionDurableShared(TestContext context) throws Exception {
    doSubscriptionConfigTestImpl(context, true, "testConfigureSubscriptionDurableShared", true, false);
  }

  @Test(timeout = 20000)
  public void testConfigureSubscriptionDurableSharedGlobal(TestContext context) throws Exception {
    doSubscriptionConfigTestImpl(context, true, "testConfigureSubscriptionDurableSharedGlobal", true, true);
  }

  private void doSubscriptionConfigTestImpl(TestContext context, boolean durable, String linkName, boolean shared, boolean global) throws InterruptedException, ExecutionException {
    server.close();

    final Async clientLinkOpenAsync = context.async();
    final Async serverLinkOpenAsync = context.async();
    final Async clientLinkCloseAsync = context.async();


    ProtonServer protonServer = null;
    try {
      protonServer = createServer((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(session -> session.open());

        serverConnection.senderOpenHandler(serverSender -> {
          serverSender.closeHandler(res -> {
            context.assertFalse(durable,"unexpected link close for durable sub");
            serverSender.close();
          });

          serverSender.detachHandler(res -> {
            context.assertTrue(durable,"unexpected link detach for non-durable sub");
            serverSender.detach();
          });

          LOG.trace("Server sender opened");
          serverSender.open();

          // Verify the link details used were as expected
          context.assertEquals(linkName, serverSender.getName(), "unexpected link name");
          context.assertNotNull(serverSender.getRemoteSource(), "source should not be null");
          org.apache.qpid.proton.amqp.messaging.Source source = (org.apache.qpid.proton.amqp.messaging.Source) serverSender.getRemoteSource();
          if(durable) {
            context.assertEquals(TerminusExpiryPolicy.NEVER, source.getExpiryPolicy(), "unexpected expiry");
            context.assertEquals(TerminusDurability.UNSETTLED_STATE, source.getDurable(), "unexpected durability");
          }
          Symbol[] capabilities = source.getCapabilities();
          if(shared && global) {
            context.assertTrue(Arrays.equals(new Symbol[] {Symbol.valueOf("shared"), Symbol.valueOf("global")}, capabilities), "Unexpected capabilities: " + Arrays.toString(capabilities));
          } else if(shared) {
            context.assertTrue(Arrays.equals(new Symbol[] {Symbol.valueOf("shared")}, capabilities), "Unexpected capabilities: " + Arrays.toString(capabilities));
          }

          serverLinkOpenAsync.complete();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        // Create publisher with given link name
        ProtonPublisherOptions options = new ProtonPublisherOptions().setLinkName(linkName);
        if(durable) {
          options.setDurable(true);
        }
        if(shared) {
          options.setShared(true);
        }

        if(global) {
          options.setGlobal(true);
        }

        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection,"myAddress", options);

        publisher.subscribe(new Subscriber<Delivery>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            clientLinkOpenAsync.complete();
            sub = s;
            s.request(1);
            sub.cancel();
          }

          @Override
          public void onNext(Delivery e) {
          }

          @Override
          public void onError(Throwable t) {
            if(!clientLinkCloseAsync.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            clientLinkCloseAsync.complete();
          }
        });
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkOpenAsync.awaitSuccess();

      clientLinkCloseAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testCreateUsingCustomSource(TestContext context) throws Exception {
    server.close();

    final Async clientLinkOpenAsync = context.async();
    final Async serverLinkOpenAsync = context.async();
    final Async clientLinkCloseAsync = context.async();

    final String dynamicAddress = "testCreateUsingCustomSource:" + UUID.randomUUID();

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
          context.assertTrue(remoteSource.getDynamic(), "expected dynamic source");
          context.assertNull(remoteSource.getAddress(), "expected no source address");

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

        // Create publisher with source configured to be dynamic
        ProtonPublisher<Delivery> publisher = ProtonStreams.createDeliveryConsumer(connection, null);
        org.apache.qpid.proton.amqp.messaging.Source source = (org.apache.qpid.proton.amqp.messaging.Source) publisher.getSource();
        source.setDynamic(true);

        publisher.subscribe(new Subscriber<Delivery>() {
          Subscription sub = null;

          @Override
          public void onSubscribe(Subscription s) {
            // Grab and verify the dynamic address details
            org.apache.qpid.proton.amqp.messaging.Source remoteSource = (org.apache.qpid.proton.amqp.messaging.Source) publisher.getRemoteSource();
            context.assertTrue(remoteSource.getDynamic(), "expected dynamic source");
            context.assertEquals(dynamicAddress, remoteSource.getAddress(), "unexpected source address");

            clientLinkOpenAsync.complete();

            sub = s;
            s.request(1);
            sub.cancel();
          }

          @Override
          public void onNext(Delivery e) {
          }

          @Override
          public void onError(Throwable t) {
            if(!clientLinkCloseAsync.isCompleted()) {
              context.fail("onError called");
            }
          }

          @Override
          public void onComplete() {
            clientLinkCloseAsync.complete();
          }
        });
      });

      serverLinkOpenAsync.awaitSuccess();
      clientLinkOpenAsync.awaitSuccess();

      clientLinkCloseAsync.awaitSuccess();
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
