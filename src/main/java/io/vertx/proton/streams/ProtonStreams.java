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

import org.apache.qpid.proton.message.Message;

import io.vertx.proton.ProtonConnection;
import io.vertx.proton.impl.ProtonConnectionImpl;
import io.vertx.proton.streams.impl.ProtonPublisherImpl;
import io.vertx.proton.streams.impl.ProtonPublisherWrapperImpl;
import io.vertx.proton.streams.impl.ProtonSubscriberImpl;
import io.vertx.proton.streams.impl.ProtonSubscriberWrapperImpl;

public class ProtonStreams {

  /**
   * Creates an AMQP consumer, presented as a reactive streams {@link org.reactivestreams.Publisher Publisher}.
   * Messages are carried by {@link Delivery} elements of the stream, which are used by the consuming application
   * to explicitly acknowledge each message after processing it.
   *
   * The publisher may only be subscribed to a single time.
   * Must be called on the {@link io.vertx.core.Context} thread for the given connection.
   *
   * @param connection
   *          the connection to create the consumer with.
   * @param address
   *          The source address to attach the consumer to.
   * @return the consumers Publisher stream.
   */
  public static ProtonPublisher<Delivery> createDeliveryConsumer(ProtonConnection connection, String address) {
    return createDeliveryConsumer(connection, address, new ProtonPublisherOptions());
  }

  /**
   * Creates an AMQP consumer, presented as a reactive streams {@link org.reactivestreams.Publisher Publisher}.
   * Messages are carried by {@link Delivery} elements of the stream, which are used by the consuming
   * application to explicitly acknowledge each message after processing it.
   *
   * The publisher may only be subscribed to a single time.
   * Must be called on the {@link io.vertx.core.Context} thread for the given connection.
   *
   * The consumer link is closed when the subscription is cancelled, unless the passed options request a
   * durable sub, in which case the link is only detached. A Dynamic address can be requested by setting
   * the dynamic option true.
   *
   * @param connection
   *          the connection to create the consumer with.
   * @param address
   *          The source address to attach the consumer to, or null the 'dynamic' option is being used.
   * @param options
   *          The options.
   * @return the consumers Publisher stream.
   */
  public static ProtonPublisher<Delivery> createDeliveryConsumer(ProtonConnection connection, String address, ProtonPublisherOptions options) {
    return new ProtonPublisherImpl(address, (ProtonConnectionImpl) connection, options);
  }

  /**
   * Creates an AMQP consumer, presented as a reactive streams {@link org.reactivestreams.Publisher Publisher}.
   * Messages will be automatically accepted when the {@link org.reactivestreams.Subscriber#onNext(Object) Subscriber#onNext(Object)}
   * method returns. If you require more control over when the message is accepted, you should use
   * {@link #createDeliveryConsumer(ProtonConnection, String)} instead.
   *
   * The publisher may only be subscribed to a single time.
   * Must be called on the {@link io.vertx.core.Context} thread for the given connection.
   *
   * @param connection
   *          the connection to create the consumer with.
   * @param address
   *          The source address to attach the consumer to.
   * @return the consumers Publisher stream.
   */
  public static ProtonPublisher<Message> createConsumer(ProtonConnection connection, String address) {
    return createConsumer(connection, address, new ProtonPublisherOptions());
  }

  /**
   * Creates an AMQP consumer, presented as a reactive streams {@link org.reactivestreams.Publisher Publisher}.
   * Messages will be automatically accepted when the {@link org.reactivestreams.Subscriber#onNext(Object) Subscriber#onNext(Object)}
   * method returns. If you require more control over when the message is accepted, you should use
   * {@link #createDeliveryConsumer(ProtonConnection, String, ProtonPublisherOptions)} instead.
   *
   * The publisher may only be subscribed to a single time.
   * Must be called on the {@link io.vertx.core.Context} thread for the given connection.
   *
   * @param connection
   *          the connection to create the consumer with.
   * @param address
   *          The source address to attach the consumer to.
   * @param options
   *          The options.
   * @return the consumers Publisher stream.
   */
  public static ProtonPublisher<Message> createConsumer(ProtonConnection connection, String address, ProtonPublisherOptions options) {
    ProtonPublisherImpl publisher = new ProtonPublisherImpl(address, (ProtonConnectionImpl) connection, options);

    return new ProtonPublisherWrapperImpl(publisher);
  }

  /**
   * Creates an AMQP producer, presented as a reactive streams {@link org.reactivestreams.Subscriber Subscriber}.
   *
   * The status of the message delivery, i.e whether the server peer accepts it etc, can be checked
   * using its containing tracker, which are created using {@link Tracker#create(Message, io.vertx.core.Handler)}
   * or {@link Tracker#create(Message)}.
   *
   * The subscriber may only be subscribed once.
   * Must be called on the {@link io.vertx.core.Context} thread for the given connection.
   *
   * If no address (i.e null) is specified then a producer will be established to the 'anonymous relay'
   * and each message sent must specify its individual destination address.
   *
   * @param connection
   *          the connection to create the consumer with.
   * @param address
   *          The target address to attach the producer to (or null to send to the anonymous relay).
   * @return the producers Subscriber stream.
   */
  public static ProtonSubscriber<Tracker> createTrackerProducer(ProtonConnection connection, String address) {
    return new ProtonSubscriberImpl(address, (ProtonConnectionImpl) connection);
  }

  /**
   * Creates an AMQP producer, presented as a reactive streams {@link org.reactivestreams.Subscriber Subscriber}.
   *
   * The status of the message delivery, i.e whether the server peer accepts it etc, can be checked
   * using its containing tracker, which are created using {@link Tracker#create(Message, io.vertx.core.Handler)}
   * or {@link Tracker#create(Message)}.
   *
   * The subscriber may only be subscribed once.
   * Must be called on the {@link io.vertx.core.Context} thread for the given connection.
   *
   * If no address (i.e null) is specified then a producer will be established to the 'anonymous relay'
   * and each message sent must specify its individual destination address.
   *
   * @param connection
   *          the connection to create the consumer with.
   * @param address
   *          The target address to attach the producer to (or null to send to the anonymous relay).
   * @param options
   *          The options.
   * @return the producers Subscriber stream.
   */
  public static ProtonSubscriber<Tracker> createTrackerProducer(ProtonConnection connection, String address, ProtonSubscriberOptions options) {
    return new ProtonSubscriberImpl(address, (ProtonConnectionImpl) connection, options);
  }

  /**
   * Creates an AMQP producer, presented as a reactive streams {@link org.reactivestreams.Subscriber Subscriber}.
   * The status of the message delivery can not be tracked after send, if you need that ability use
   * {@link ProtonStreams#createTrackerProducer(ProtonConnection, String)}.
   *
   * The subscriber may only be subscribed once.
   * Must be called on the {@link io.vertx.core.Context} thread for the given connection.
   *
   * If no address (i.e null) is specified then a producer will be established to the 'anonymous relay'
   * and each message sent must specify its individual destination address.
   *
   * @param connection
   *          the connection to create the consumer with.
   * @param address
   *          The target address to attach the producer to (or null to send to the anonymous relay).
   * @return the producers Subscriber stream.
   */
  public static ProtonSubscriber<Message> createProducer(ProtonConnection connection, String address) {
    ProtonSubscriberImpl subscriber = new ProtonSubscriberImpl(address, (ProtonConnectionImpl) connection);

    return new ProtonSubscriberWrapperImpl(subscriber);
  }

  /**
   * Creates an AMQP producer, presented as a reactive streams {@link org.reactivestreams.Subscriber Subscriber}.
   * The status of the message delivery can not be tracked after send, if you need that ability use
   * {@link ProtonStreams#createTrackerProducer(ProtonConnection, String, ProtonSubscriberOptions)}.
   *
   * The subscriber may only be subscribed once.
   * Must be called on the {@link io.vertx.core.Context} thread for the given connection.
   *
   * If no address (i.e null) is specified then a producer will be established to the 'anonymous relay'
   * and each message sent must specify its individual destination address.
   *
   * @param connection
   *          the connection to create the consumer with.
   * @param address
   *          The target address to attach the producer to (or null to send to the anonymous relay).
   * @param options
   *          The options.
   * @return the producers Subscriber stream.
   */
  public static ProtonSubscriber<Message> createProducer(ProtonConnection connection, String address, ProtonSubscriberOptions options) {
    ProtonSubscriberImpl subscriber = new ProtonSubscriberImpl(address, (ProtonConnectionImpl) connection, options);

    return new ProtonSubscriberWrapperImpl(subscriber);
  }
}
