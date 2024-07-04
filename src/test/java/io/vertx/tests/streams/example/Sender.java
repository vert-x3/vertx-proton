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
package io.vertx.tests.streams.example;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.streams.ProtonStreams;

public class Sender {
  private static final int COUNT = 100;

  public static void main(String[] args) throws Exception {
    try {
      Vertx vertx = Vertx.vertx();
      ProtonClient client = ProtonClient.create(vertx);

      // Connect, then use the event loop thread to process the connection
      client.connect("localhost", 5672, res -> {
        Context ctx = Vertx.currentContext();
        if (res.succeeded()) {
          System.out.println("We're connected");

          ProtonConnection connection = res.result();
          connection.open();

          Subscriber<Message> producerSubscriber = ProtonStreams.createProducer(connection, "queue");

          Publisher<Message> publisher = Flowable.range(1, COUNT).map(i -> {
            Message m = Proton.message();
            m.setBody(new AmqpValue("Hello " + i));
            return m;
          }).doFinally(() -> {
            ctx.runOnContext(x -> {
              System.out.println("Publisher finished, closing connection.");
              connection.closeHandler(y -> {
                System.out.println("Connection closed.");
                connection.disconnect();
                vertx.close();
              }).close();
            });
          });

          publisher.subscribe(producerSubscriber);
        } else {
          System.out.println("Failed to connect, exiting: " + res.cause());
          System.exit(1);
        }
      });
    } catch (Exception exp) {
      System.out.println("Caught exception, exiting.");
      exp.printStackTrace(System.out);
      System.exit(1);
    }
  }
}
