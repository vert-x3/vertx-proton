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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.streams.ProtonStreams;

public class Receiver {

  private static final int COUNT = 100;
  private static final int REQ_WINDOW = 10; // Needs to divide count exactly

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

          Publisher<Message> consumerPublisher = ProtonStreams.createConsumer(connection, "queue");

          Subscriber<Message> subscriber = new Subscriber<Message>() {
            AtomicInteger receievedCount = new AtomicInteger();
            Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
              subscription = s;
              // Initial request for some messages
              subscription.request(REQ_WINDOW);
            }

            @Override
            public void onNext(Message env) {
              int msgNum = receievedCount.incrementAndGet();

              String content = (String) ((AmqpValue) env.getBody()).getValue();
              System.out.println("Received message: " + content);

              if(msgNum % REQ_WINDOW == 0 && msgNum < COUNT) {
                // Request more messages
                subscription.request(REQ_WINDOW);
              } else if (msgNum == COUNT) {
                // Done, clean up.
                subscription.cancel();
                closeConnection(vertx, connection);
              }
            }

            @Override
            public void onError(Throwable t) {
              System.out.println("onError(): " + t);
              closeConnection(vertx, connection);
            }

            @Override
            public void onComplete() {
              System.out.println("onComplete()");
              closeConnection(vertx, connection);
            }

            private void closeConnection(Vertx vertx, ProtonConnection connection) {
              ctx.runOnContext(x -> {
                System.out.println("Subscriber finished, closing connection.");
                connection.closeHandler(y -> {
                  System.out.println("Connection closed.");
                  connection.disconnect();
                  vertx.close();
                }).close();
              });
            }
          };

          consumerPublisher.subscribe(subscriber);
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
