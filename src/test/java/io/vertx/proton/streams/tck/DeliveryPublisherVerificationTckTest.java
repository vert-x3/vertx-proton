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
package io.vertx.proton.streams.tck;

import static io.vertx.proton.ProtonHelper.message;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.proton.streams.Delivery;
import io.vertx.proton.streams.ProtonPublisher;
import io.vertx.proton.streams.ProtonStreams;
import io.vertx.proton.streams.impl.ProtonPublisherImpl;
import io.vertx.proton.MockServer;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;

public class DeliveryPublisherVerificationTckTest extends PublisherVerification<Delivery> {

  private static final Logger LOG = LoggerFactory.getLogger(DeliveryPublisherVerificationTckTest.class);

  private static final long DEFAULT_TIMEOUT = 200L;
  private static final long DEFAULT_GC_TIMEOUT = 300L;

  protected TestServer server;
  Vertx vertx = Vertx.vertx();
  protected String testName = "unknown";

  public DeliveryPublisherVerificationTckTest() {
    super(new TestEnvironment(DEFAULT_TIMEOUT), DEFAULT_GC_TIMEOUT);
  }

  @Override public long maxElementsFromPublisher() {
    return publisherUnableToSignalOnComplete(); // == Long.MAX_VALUE == unbounded
  }

  @BeforeMethod
  public void testName(Method testMethod) throws Exception {
    testName = testMethod.getName();
    LOG.trace("#### Test: " + testName);
  }

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    try {
      server = createServer();
    } catch (Exception e1) {
      throw new RuntimeException("Problem creating test server", e1);
    }
  }

  @AfterMethod
  public void tearDown() throws Exception {
    if(server != null) {
      server.close();
      server = null;
    }
  }

  @Override
  public Publisher<Delivery> createPublisher(long elements) {
    int actualPort = server.actualPort();

    ProtonClient client = ProtonClient.create(vertx);

    AtomicReference<Publisher<Delivery>> ref = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    client.connect("localhost", actualPort, result -> {
      if (result.succeeded()) {
        ProtonConnection conn = result.result();
        conn.open();

        ProtonPublisher<Delivery> stream = ProtonStreams.createDeliveryConsumer(conn, testName);
        ((ProtonPublisherImpl) stream).setEmitOnConnectionEnd(false);
        ref.set(stream);
      } else {
        LOG.error("Connection failed");
      }

      latch.countDown();
    });

    try {
      LOG.trace("Awaiting connection");
      boolean res = latch.await(2, TimeUnit.SECONDS);
      LOG.trace("Client connected: " + res);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while creating publisher", e);
    }

    return ref.get();
  }

  @Override
  public Publisher<Delivery> createFailedPublisher() {
    server.setDetachLink(true);
    return createPublisher(0);
  }

  private TestServer createServer() throws Exception {
    return new TestServer(vertx, (connection) -> {
      connection.openHandler(res -> {
        LOG.trace("Client connected: " + connection.getRemoteContainer());
        connection.open();
      }).closeHandler(c -> {
        LOG.trace("Client closing amqp connection: " + connection.getRemoteContainer());
        connection.close();
        connection.disconnect();
      }).disconnectHandler(c -> {
        LOG.trace("Client socket disconnected: " + connection.getRemoteContainer());
        connection.disconnect();
      })
      .sessionOpenHandler(session -> session.open());

      connection.senderOpenHandler(sender -> {
        if(!server.getDetachLink()) {
          LOG.trace("Sending to client from: " + sender.getRemoteSource().getAddress());
          sender.setSource(sender.getRemoteSource()); // This is rather naive, for example use only, proper
                                                      // servers should ensure that they advertise their own
                                                      // Source settings that actually reflect what is in place.
                                                      // The request may have also been for a dynamic address.
          AtomicLong count = new AtomicLong();
          AtomicLong outstanding = new AtomicLong();
          sender.sendQueueDrainHandler(s -> {
            while(!s.sendQueueFull()) {
              LOG.trace("Sending message to client");
              Message m = message("Hello World from Server!" + count.incrementAndGet());
              outstanding.incrementAndGet();
              s.send(m, delivery -> {
                LOG.trace("The message was received by the client.");
              });
            }
          });

          sender.closeHandler(s -> {
            s.result().close();
          });
        }

        sender.open();

        if(server.getDetachLink()) {
          sender.setCondition(new ErrorCondition(Symbol.getSymbol("Failed Publisher Requested"), ""));
          sender.close();
        }
      });
    });
  }

  protected class TestServer extends MockServer {
    private final AtomicBoolean detachLink = new AtomicBoolean();

    public TestServer(Vertx vertx, Handler<ProtonConnection> connectionHandler) throws ExecutionException, InterruptedException {
      super(vertx, connectionHandler);
    }

    boolean getDetachLink() {
      return detachLink.get();
    }

    void setDetachLink(boolean detachLink) {
      this.detachLink.set(detachLink);
    }
  }
}
