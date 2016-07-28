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
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.util.concurrent.ExecutionException;

import static io.vertx.proton.ProtonHelper.condition;
import static io.vertx.proton.ProtonHelper.message;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MockServer {

  private ProtonServer server;
  private ProtonSender echoSender;
  private volatile int credits = 1000;

  // Toggle to reuse a fixed port, e.g for capture.
  private int bindPort = 0;
  private boolean reuseAddress = false;

  enum Addresses {
    command, drop, echo, // Echos message back to consumer at address "echo"
    no_messages, two_messages, five_messages
  }

  enum Commands {
    disconnect
  }

  public MockServer(Vertx vertx) throws ExecutionException, InterruptedException {
    this(vertx, null);
  }

  public MockServer(Vertx vertx, Handler<ProtonConnection> connectionHandler) throws ExecutionException, InterruptedException {
    if(connectionHandler == null) {
      connectionHandler = (connection) -> processConnection(vertx, connection);
    }

    ProtonServerOptions protonServerOptions = new ProtonServerOptions();
    protonServerOptions.setReuseAddress(reuseAddress);
    server = ProtonServer.create(vertx, protonServerOptions);
    server.connectHandler(connectionHandler);
    FutureHandler<ProtonServer, AsyncResult<ProtonServer>> handler = FutureHandler.asyncResult();
    server.listen(bindPort, handler);
    handler.get();
  }

  ProtonServer getProtonServer() {
    return server;
  }

  private void processConnection(Vertx vertx, ProtonConnection connection) {
    connection.sessionOpenHandler(session -> session.open());
    connection.receiverOpenHandler(receiver -> {
      receiver.handler((delivery, msg) -> {
        String address = msg.getAddress();
        if (address == null) {
          address = receiver.getRemoteTarget().getAddress();
        }
        processMessage(connection, receiver, delivery, msg, address);
      }).setPrefetch(credits).open();
    });
    connection.senderOpenHandler(sender -> {
      Addresses address = null;
      if (sender.getRemoteSource() != null) {
        address = Addresses.valueOf(sender.getRemoteSource().getAddress());
        switch (address) {
        case two_messages: {
          sender.open();
          sender.send(message("1"));
          sender.send(message("2"), d -> {
            sender.close();
          });
          break;
        }
        case five_messages: {
          sender.open();
          sender.send(message("1"));
          sender.send(message("2"));
          sender.send(message("3"));
          sender.send(message("4"));
          sender.send(message("5"), d -> {
            sender.close();
          });
          break;
        }
        case echo: {
          if (echoSender == null) {
            sender.open();
            echoSender = sender;
            // TODO: set the source/target appropriately
          } else {
            sender.setCondition(condition(AmqpError.ILLEGAL_STATE.toString(), "Already have echo recipient"));
            sender.close();
          }
          break;
        }
        case drop: // fall through
        case no_messages: {
          sender.open();
          break;
        }
        default:
          sender.setCondition(condition(AmqpError.NOT_FOUND, "unknown address")).close();
        }
      }
    });
    connection.openHandler(result -> {
      connection.setContainer("pong: " + connection.getRemoteContainer()).open();
    });

  }

  public int getProducerCredits() {
    return credits;
  }

  public void setProducerCredits(int credits) {
    this.credits = credits;
  }

  public void close() {
    server.close();
  }

  public int actualPort() {
    return server.actualPort();
  }

  private void processMessage(ProtonConnection connection, ProtonReceiver receiver, ProtonDelivery delivery,
                              Message msg, String to) {
    switch (Addresses.valueOf(to)) {

    case drop: {
      break;
    }

    case echo: {
      if (echoSender != null) {
        echoSender.send(delivery.getTag(), msg);
      } else {
        // TODO
      }
      break;
    }

    case command: {
      String command = (String) ((AmqpValue) msg.getBody()).getValue();
      switch (Commands.valueOf(command)) {
      case disconnect:
        connection.disconnect();
        break;
      }
      break;
    }
    }
  }

}
