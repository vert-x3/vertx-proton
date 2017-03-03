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
package io.vertx.proton.example;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import static io.vertx.proton.ProtonHelper.message;
import static io.vertx.proton.ProtonHelper.tag;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class HelloWorldServer {

  public static void main(String[] args) {

    // Create the Vert.x instance
    Vertx vertx = Vertx.vertx();

    // Create the Vert.x AMQP client instance
    ProtonServer server = ProtonServer.create(vertx).connectHandler((connection) -> {
      helloProcessConnection(vertx, connection);
    }).listen(5672, (res) -> {
      if (res.succeeded()) {
        System.out.println("Listening on: " + res.result().actualPort());
      } else {
        res.cause().printStackTrace();
      }
    });

    // Just stop main() from exiting
    try {
      System.in.read();
    } catch (Exception ignore) {
    }
  }

  private static void helloProcessConnection(Vertx vertx, ProtonConnection connection) {
    connection.openHandler(res -> {
      System.out.println("Client connected: " + connection.getRemoteContainer());
      connection.open();
    }).closeHandler(c -> {
      System.out.println("Client closing amqp connection: " + connection.getRemoteContainer());
      connection.close();
      connection.disconnect();
    }).disconnectHandler(c -> {
      System.out.println("Client socket disconnected: " + connection.getRemoteContainer());
      connection.disconnect();
    })
    .sessionOpenHandler(session -> session.open());

    connection.receiverOpenHandler(receiver -> {
      receiver.setTarget(receiver.getRemoteTarget()) // This is rather naive, for example use only, proper
                                                     // servers should ensure that they advertise their own
                                                     // Target settings that actually reflect what is in place.
                                                     // The request may have also been for a dynamic address.
          .handler((delivery, msg) -> {

            String address = msg.getAddress();
            if (address == null) {
              address = receiver.getRemoteTarget().getAddress();
            }

            Section body = msg.getBody();
            if (body instanceof AmqpValue) {
              String content = (String) ((AmqpValue) body).getValue();
              System.out.println("message to:" + address + ", body: " + content);
            }
          }).open();
    });

    connection.senderOpenHandler(sender -> {
      System.out.println("Sending to client from: " + sender.getRemoteSource().getAddress());
      sender.setSource(sender.getRemoteSource()); // This is rather naive, for example use only, proper
                                                  // servers should ensure that they advertise their own
                                                  // Source settings that actually reflect what is in place.
                                                  // The request may have also been for a dynamic address.
      sender.open();
      vertx.setPeriodic(1000, timer -> {
        if (connection.isDisconnected()) {
          vertx.cancelTimer(timer);
        } else {
          System.out.println("Sending message to client");
          Message m = message("Hello World from Server!");
          sender.send(m, delivery -> {
            System.out.println("The message was received by the client.");
          });
        }
      });
    });

  }

}
