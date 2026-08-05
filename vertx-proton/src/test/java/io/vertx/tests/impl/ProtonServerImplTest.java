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
package io.vertx.tests.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import io.vertx.proton.sasl.ProtonSaslAuthenticatorFactory;
import io.vertx.tests.FutureHandler;
import io.vertx.tests.TestSupport;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.Sasl.SaslOutcome;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.vertx.tests.TestSupport.prepareMessageWithZeroWidthArrayElements;
import static io.vertx.tests.TestSupport.validateMessage;
import static io.vertx.tests.TestSupport.validateMessageArray;
import static io.vertx.tests.TestSupport.createServer;
import static io.vertx.tests.TestSupport.prepareMessageWithDecodeDepth;

@RunWith(VertxUnitRunner.class)
public class ProtonServerImplTest {

  private static Logger LOG = LoggerFactory.getLogger(ProtonServerImplTest.class);

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

  @Test(timeout = 20000)
  public void testZeroWidthArrayElementsWithDefaultLimit(TestContext context) throws Exception {
    ProtonServerOptions options = new ProtonServerOptions();

    doZeroWidthArrayElementsWithLimitTestImpl(context, options, 1, true);
  }

  @Test(timeout = 20000)
  public void testZeroWidthArrayElementsWithLimitSetPermissive(TestContext context) throws Exception {
    // As above, but options set to allow it to succeed
    ProtonServerOptions options = new ProtonServerOptions();
    options.setMessageZeroWidthArrayElementLimit(1);

    doZeroWidthArrayElementsWithLimitTestImpl(context, options, 1, false);
  }

  @Test(timeout = 20000)
  public void testZeroWidthArrayElementsWithLimitSetPermissive2(TestContext context) throws Exception {
    // Similar but a larger array
    int zeroWidthArrayElementCount = 100;
    ProtonServerOptions options = new ProtonServerOptions();
    options.setMessageZeroWidthArrayElementLimit(zeroWidthArrayElementCount);

    doZeroWidthArrayElementsWithLimitTestImpl(context, options, zeroWidthArrayElementCount, false);
  }

  @Test(timeout = 20000)
  public void testZeroWidthArrayElementsWithLimitSetRestricted(TestContext context) throws Exception {
    // Similar but with options set to block it
    int zeroWidthArrayElementCount = 99;
    ProtonServerOptions options = new ProtonServerOptions();
    options.setMessageZeroWidthArrayElementLimit(zeroWidthArrayElementCount);

    doZeroWidthArrayElementsWithLimitTestImpl(context, options, zeroWidthArrayElementCount + 1, true);
  }

  private void doZeroWidthArrayElementsWithLimitTestImpl(TestContext context, ProtonServerOptions options,
                                                         int elementCount, boolean expectDecodeFailure) throws Exception {
    Async serverAsync = context.async();
    Async clientAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer(vertx, options, (serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });
        serverConnection.sessionOpenHandler(session -> {
          session.open();
        });

        serverConnection.receiverOpenHandler(serverReceiver -> {
          AtomicInteger counter = new AtomicInteger(0);

          serverReceiver.handler((d, m) -> {
            int count = counter.incrementAndGet();
            switch (count) {
              case 1:
                validateMessageArray(context, 1, new boolean[0], m);
                break;
              case 2:
                if(expectDecodeFailure) {
                  validateMessageArray(context, 3, new boolean[0], m);
                  serverAsync.complete();
                } else {
                  boolean[] expected = new boolean[elementCount];
                  Arrays.fill(expected, true);

                  validateMessageArray(context, 2, expected, m);
                }
                break;
              case 3:
                if(expectDecodeFailure) {
                  context.fail("should not get third message");
                } else {
                  validateMessageArray(context, 3, new boolean[0], m);
                  serverAsync.complete();
                }
                break;
            }
          }).open();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        ProtonSender sender = connection.createSender("address");

        AtomicInteger count = new AtomicInteger();
        sender.sendQueueDrainHandler(s -> {
          int msg = count.incrementAndGet();

          switch (msg) {
            case 1:
              context.assertEquals(1000, s.getCredit(), "Unexpected initial credit level when send handler fired for round 1");

              MessageImpl message1 = prepareMessageWithZeroWidthArrayElements(context, 0);
              sender.send(message1, del -> {
                context.assertTrue(del.getRemoteState() instanceof Accepted, "Unexpected state for delivery 1 after update");
              });
              break;
            case 2:
              MessageImpl message2 = prepareMessageWithZeroWidthArrayElements(context, elementCount);
              sender.send(message2, del -> {
                DeliveryState state = del.getRemoteState();
                if(expectDecodeFailure) {
                  context.assertTrue(state instanceof Modified, "Unexpected state for delivery 2 after update");
                  context.assertTrue(((Modified)state).getDeliveryFailed(), "Expected true");
                  context.assertTrue(((Modified)state).getUndeliverableHere(), "Expected true");
                } else {
                  context.assertTrue(del.getRemoteState() instanceof Accepted, "Unexpected state for delivery 2 after update");
                }
              });
              break;
            case 3:
              MessageImpl message3 = prepareMessageWithZeroWidthArrayElements(context, 0);
              sender.send(message3, del -> {
                context.assertTrue(del.getRemoteState() instanceof Accepted, "Unexpected state for delivery 3 after update");
                // We've sent 3 messages, consumer should receive 2 and fail 1, or get all 3, but always restore credit for all.
                // Verify credit is fully replenished to initial 1000 in the end.
                vertx.setTimer(500, x -> {
                  context.assertEquals(1000, s.getCredit(), "Unexpected credit level after messages processed");
                  clientAsync.complete();
                });
              });
              break;
          }
        });
        sender.open();
      });

      clientAsync.awaitSuccess();
      serverAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testDecodeDepthWithDefaultLimitAllowed(TestContext context) throws Exception {
    ProtonServerOptions options = new ProtonServerOptions();

    doDecodeDepthWithLimitTestImpl(context, options, 32, false);
  }

  @Test(timeout = 20000)
  public void testDecodeDepthWithDefaultLimit(TestContext context) throws Exception {
    ProtonServerOptions options = new ProtonServerOptions();

    doDecodeDepthWithLimitTestImpl(context, options, 33, true);
  }

  @Test(timeout = 20000)
  public void testDecodeDepthWithLimitSetPermissive(TestContext context) throws Exception {
    // As above, but options set to allow it to succeed
    ProtonServerOptions options = new ProtonServerOptions();
    options.setMessageMaxDecodeDepth(33);

    doDecodeDepthWithLimitTestImpl(context, options, 33, false);
  }

  @Test(timeout = 20000)
  public void testDecodeDepthWithLimitSetRestricted(TestContext context) throws Exception {
    // Lower value with options set to block it
    int depth = 2;
    ProtonServerOptions options = new ProtonServerOptions();
    options.setMessageMaxDecodeDepth(depth);

    doDecodeDepthWithLimitTestImpl(context, options, depth + 1, true);
  }

  private void doDecodeDepthWithLimitTestImpl(TestContext context, ProtonServerOptions options,
                                              int depth, boolean expectDecodeFailure) throws Exception {
    Async serverAsync = context.async();
    Async clientAsync = context.async();

    ProtonServer protonServer = null;
    try {
      protonServer = createServer(vertx, options, (serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });
        serverConnection.sessionOpenHandler(session -> {
          session.open();
        });

        serverConnection.receiverOpenHandler(serverReceiver -> {
          AtomicInteger counter = new AtomicInteger(0);

          serverReceiver.handler((d, m) -> {
            int count = counter.incrementAndGet();
            switch (count) {
            case 1:
              validateMessage(context, 1, new ArrayList<Object>(), m);
              break;
            case 2:
              if(expectDecodeFailure) {
                validateMessage(context, 3, new ArrayList<Object>(), m);
                serverAsync.complete();
              } else {
                List<Object> expected = TestSupport.prepareNestedLists(depth);

                validateMessage(context, 2, expected, m);
              }
              break;
            case 3:
              if(expectDecodeFailure) {
                context.fail("should not get third message");
              } else {
                validateMessage(context, 3, new ArrayList<Object>(), m);
                serverAsync.complete();
              }
              break;
            }
          }).open();
        });
      });

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);
      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();
        connection.open();

        ProtonSender sender = connection.createSender("address");

        AtomicInteger count = new AtomicInteger();
        sender.sendQueueDrainHandler(s -> {
          int msg = count.incrementAndGet();

          switch (msg) {
          case 1:
            context.assertEquals(1000, s.getCredit(), "Unexpected initial credit level when send handler fired for round 1");

            Message message1 = prepareMessageWithDecodeDepth(context, 1);
            sender.send(message1, del -> {
              context.assertTrue(del.getRemoteState() instanceof Accepted, "Unexpected state for delivery 1 after update");
            });
            break;
          case 2:
            Message message2 = prepareMessageWithDecodeDepth(context, depth);
            sender.send(message2, del -> {
              DeliveryState state = del.getRemoteState();
              if(expectDecodeFailure) {
                context.assertTrue(state instanceof Modified, "Unexpected state for delivery 2 after update");
                context.assertTrue(((Modified)state).getDeliveryFailed(), "Expected true");
                context.assertTrue(((Modified)state).getUndeliverableHere(), "Expected true");
              } else {
                context.assertTrue(del.getRemoteState() instanceof Accepted, "Unexpected state for delivery 2 after update");
              }
            });
            break;
          case 3:
            Message message3 = prepareMessageWithDecodeDepth(context, 1);
            sender.send(message3, del -> {
              context.assertTrue(del.getRemoteState() instanceof Accepted, "Unexpected state for delivery 3 after update");
              // We've sent 3 messages, consumer should receive 2 and fail 1, or get all 3, but always restore credit for all.
              // Verify credit is fully replenished to initial 1000 in the end.
              vertx.setTimer(500, x -> {
                context.assertEquals(1000, s.getCredit(), "Unexpected credit level after messages processed");
                clientAsync.complete();
              });
            });
            break;
          }
        }).open();
      });

      clientAsync.awaitSuccess();
      serverAsync.awaitSuccess();
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

  @Test(timeout = 20000)
  public void testExceedingMaxTransfersPerDelivery(TestContext context) throws Exception {
    final int maxFrameSize = 10000;
    final int dataPayloadSize = maxFrameSize + 100;
    Async serverReceiverOpenAsync = context.async();
    Async clientSenderCreditCheck = context.async();
    Async clientSenderOpenAsync = context.async();
    Async clientConnectionDisconnectCheck = context.async();
    Async clientConnectionCloseCheck = context.async();
    AtomicBoolean messageHandlerFired = new AtomicBoolean(false);

    ProtonServer protonServer = null;
    try {
      ProtonServerOptions serverOptions = new ProtonServerOptions();
      serverOptions.setMaxFrameSize(maxFrameSize);
      serverOptions.setMaxTransfersPerDelivery(1);

      protonServer = ProtonServer.create(vertx, serverOptions).connectHandler((serverConnection) -> {
        serverConnection.openHandler(result -> {
          serverConnection.open();
        });

        serverConnection.sessionOpenHandler(serverSession -> {
          serverSession.open();
        });

        serverConnection.receiverOpenHandler(reciever -> {
          reciever.handler((delivery, message) -> {
            messageHandlerFired.set(true);
          });

          LOG.trace("Server receiver opened");
          reciever.open();
          serverReceiverOpenAsync.complete();
        });
      });

      FutureHandler<ProtonServer, AsyncResult<ProtonServer>> handler = FutureHandler.asyncResult();
      protonServer.listen(0, handler);
      handler.get();

      // ===== Client Handling =====

      ProtonClient client = ProtonClient.create(vertx);

      client.connect("localhost", protonServer.actualPort(), res -> {
        context.assertTrue(res.succeeded());

        ProtonConnection connection = res.result();

        connection.disconnectHandler(conn -> {
          clientConnectionDisconnectCheck.complete();
        });

        connection.closeHandler(connectionResult -> {
          clientConnectionCloseCheck.complete();

          context.assertTrue(connectionResult.failed());
          context.assertEquals(EndpointState.CLOSED, connection.getRemoteState());
          ErrorCondition condition = connection.getRemoteCondition();

          context.assertNotNull(condition);
          String description = condition.getDescription();
          context.assertNotNull(description);
          context.assertTrue(description.contains("Max transfers per delivery limit exceeded"), "Unexpected description: " + description);
        });

        connection.openHandler(x -> {
          LOG.trace("Client connection opened");
          final ProtonSender sender = connection.createSender("some-address");

          sender.openHandler(y -> {
            LOG.trace("Client link opened");

            clientSenderOpenAsync.complete();

            sender.sendQueueDrainHandler(ss -> {
              if(!clientSenderCreditCheck.isCompleted()) {
                context.assertTrue(sender.getCredit() > 0  , "Unexpectedly low credit: " + sender.getCredit());
                clientSenderCreditCheck.complete();

                //Send message
                Message message = Message.Factory.create();
                byte[] payload = new byte[dataPayloadSize];
                for (int i = 0; i < payload.length; i++) {
                  payload[i] = (byte) (i % 256);
                }
                message.setBody(new Data(new Binary(payload)));

                sender.send(message);
              }
            });
          });
          sender.open();

        }).open();
      });

      serverReceiverOpenAsync.awaitSuccess();
      clientSenderOpenAsync.awaitSuccess();
      clientSenderCreditCheck.awaitSuccess();
      clientConnectionCloseCheck.awaitSuccess();
      clientConnectionDisconnectCheck.awaitSuccess();

      context.assertFalse(messageHandlerFired.get(), "message handler should not have fired");
    } finally {
      if (protonServer != null) {
        protonServer.close();
      }
    }
  }

}
