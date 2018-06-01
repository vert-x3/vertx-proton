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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.proton.MockServer;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.streams.ProtonSubscriber;
import io.vertx.proton.streams.impl.ProtonSubscriberImpl;
import io.vertx.proton.streams.impl.ProtonSubscriberWrapperImpl;
import io.vertx.proton.impl.ProtonConnectionImpl;

public class MessageSubscriberWhiteboxVerificationTckTest extends SubscriberWhiteboxVerification<Message> {

  private static final Logger LOG = LoggerFactory.getLogger(MessageSubscriberWhiteboxVerificationTckTest.class);

  private static final long DEFAULT_TIMEOUT = 500L;

  TestServer server;
  Vertx vertx = Vertx.vertx();

  public MessageSubscriberWhiteboxVerificationTckTest() {
    super(new TestEnvironment(DEFAULT_TIMEOUT));
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
  public Subscriber<Message> createSubscriber(WhiteboxSubscriberProbe<Message> probe) {
    int actualPort = server.actualPort();

    ProtonClient client = ProtonClient.create(vertx);

    AtomicReference<Subscriber<Message>> ref = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    client.connect("localhost", actualPort, result -> {
      if (result.succeeded()) {
        ProtonConnection conn = result.result();
        conn.open();

        // Modified stream impl, overriding methods to add test probe instrumentation
        ProtonSubscriberImpl subscriber = new ProtonSubscriberImpl("myAddress", (ProtonConnectionImpl) conn);
        ProtonSubscriber<Message> stream = new ProtonSubscriberWrapperImpl(subscriber) {
          @Override
          public void onSubscribe(final Subscription s) {
            super.onSubscribe(s);

            probe.registerOnSubscribe(new SubscriberPuppet() {
              @Override
              public void triggerRequest(long n) {
                s.request(n);
              }

              @Override
              public void signalCancel() {
                s.cancel();
              }
            });
          }

          @Override
          public void onNext(Message value) {
            probe.registerOnNext(value);
            super.onNext(value);
          }

          @Override
          public void onError(Throwable t) {
            probe.registerOnError(t);
            super.onError(t);
          }

          @Override
          public void onComplete() {
            probe.registerOnComplete();
            super.onComplete();
          }
        };

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
      throw new RuntimeException("Interrupted while creating subscriber", e);
    }
    return ref.get();
  }

  @Override
  public Message createElement(int element) {
    Message m = Proton.message();
    m.setBody(new AmqpValue("hello" + element));

    return m;
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

      connection.receiverOpenHandler(receiver -> {
        if(!server.getDetachLink()) {
          LOG.trace("Receiving from client to: " + receiver.getRemoteTarget().getAddress());
          receiver.setTarget(receiver.getRemoteTarget()); // This is rather naive, for example use only, proper
                                                          // servers should ensure that they advertise their own
                                                          // Target settings that actually reflect what is in place.
                                                          // The request may have also been for a dynamic address.
          receiver.handler((delivery, msg) -> {

            String address = msg.getAddress();
            if (address == null) {
              address = receiver.getRemoteTarget().getAddress();
            }

            Section body = msg.getBody();
            String content  = "unknown";
            if (body instanceof AmqpValue) {
              content = (String) ((AmqpValue) body).getValue();
            }

            LOG.trace("message to:" + address + ", body: " + content);
          });

          receiver.closeHandler(s -> {
            s.result().close();
          });
        }

        receiver.open();

        if(server.getDetachLink()) {
          receiver.setCondition(new ErrorCondition(Symbol.getSymbol("Failed Subscriber Requested"), ""));
          receiver.close();
        }
      });
    });
  }

  private class TestServer extends MockServer {

    private final AtomicBoolean detachLink = new AtomicBoolean();

    public TestServer(Vertx vertx, Handler<ProtonConnection> connectionHandler) throws ExecutionException, InterruptedException {
      super(vertx, connectionHandler);
    }

    boolean getDetachLink() {
      return detachLink.get();
    }
  }

}
