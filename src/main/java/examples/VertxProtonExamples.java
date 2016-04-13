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
package examples;

import static io.vertx.proton.ProtonHelper.message;

import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import io.vertx.core.Vertx;
import io.vertx.docgen.Source;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;

@Source(translate = false)
public class VertxProtonExamples {

  public void example1(Vertx vertx) {
    ProtonClient client = ProtonClient.create(vertx);

    // Connect, then use the event loop thread to process things thereafter
    client.connect("hostname", 5672, "username", "password", connectResult -> {
      if (connectResult.succeeded()) {
        connectResult.result().setContainer("my-container/client-id").openHandler(openResult -> {
          if (openResult.succeeded()) {
            ProtonConnection conn = openResult.result();
            // Create senders, receivers etc..
          }
        }).open();
      }
    });
  }

  public void example2(ProtonConnection connection) {
    connection.createSender("myQueue").openHandler(openResult -> {
      if (openResult.succeeded()) {
        ProtonSender sender = openResult.result();

        Message message = message();
        message.setBody(new AmqpValue("Hello World"));

        // Send message, providing an onUpdated delivery handler that prints updates
        sender.send(message, delivery -> {
          System.out.println(String.format("Message received by server: remote state=%s, remotely settled=%s",
                                           delivery.getRemoteState(), delivery.remotelySettled()));
        });
      }
    }).open();
  }

  public void example3(ProtonConnection connection) {
    connection.createReceiver("myQueue").handler((delivery, msg) -> {
      Section body = msg.getBody();
      if (body instanceof AmqpValue) {
        System.out.println("Received message with content: " + ((AmqpValue) body).getValue());
      }
      // By default, the receiver automatically accepts (and settles) the delivery
      // when the handler returns if no other disposition has already been applied.
    }).open();
  }
}
