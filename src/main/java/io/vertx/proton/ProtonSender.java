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

import io.vertx.core.Handler;

import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonSender extends ProtonLink<ProtonSender> {

  /**
   * Send the given message.
   *
   * @param message
   *          the message to send
   * @return the delivery used to send the message
   */
  ProtonDelivery send(Message message);

  /**
   * Send the given message, registering the given handler to be called whenever the related delivery is updated due to
   * receiving disposition frames from the peer.
   *
   * @param message
   *          the message to send
   * @param onUpdated
   *          handler called when a disposition update is received for the delivery
   * @return the delivery used to send the message
   */
  ProtonDelivery send(Message message, Handler<ProtonDelivery> onUpdated);

  /**
   * Send the given message, using the supplied delivery tag when creating the delivery.
   *
   * @param tag
   *          the tag to use for the delivery used to send the message
   * @param message
   *          the message to send
   * @return the delivery used to send the message
   */
  ProtonDelivery send(byte[] tag, Message message);

  /**
   * Send the given message, using the supplied delivery tag when creating the delivery, and registering the given
   * handler to be called whenever the related delivery is updated due to receiving disposition frames from the peer.
   *
   * @param tag
   *          the tag to use for the delivery used to send the message
   * @param message
   *          the message to send
   * @param onUpdated
   *          handler called when a disposition update is received for the delivery
   * @return the delivery used to send the message
   */
  ProtonDelivery send(byte[] tag, Message message, Handler<ProtonDelivery> onUpdated);

  /**
   * Gets whether the senders outgoing send queue is full, i.e. there is currently no credit to send and send
   * operations will actually buffer locally until there is.
   *
   * @return whether the send queue is full
   */
  boolean sendQueueFull();

  /**
   * Sets a handler called when the send queue is not full, i.e. there is credit available to send messages.
   *
   * @param handler
   *          the handler to process messages
   * @return the sender
   */
  ProtonSender sendQueueDrainHandler(Handler<ProtonSender> handler);

  /**
   * Sets whether sent deliveries should be automatically locally-settled once they have become remotely-settled by the
   * receiving peer.
   *
   * True by default.
   *
   * @param autoSettle
   *          whether deliveries should be auto settled locally after being settled by the receiver
   * @return the sender
   */
  ProtonSender setAutoSettle(boolean autoSettle);

  /**
   * Get whether the receiver is auto settling deliveries.
   *
   * @return whether deliveries should be auto settled locally after being settled by the receiver
   * @see #setAutoSettle(boolean)
   */
  boolean isAutoSettle();

  /**
   * Sets whether the link is automatically marked {@link #drained()} after the send queue drain handler callback
   * returns if the receiving peer requested that credit be drained, as indicated by the value of the
   * {@link #getDrain()} flag.
   *
   * True by default.
   *
   * @param autoDrained
   *          whether the link will automatically be drained after the send queue drain handler fires in drain mode
   * @return the sender
   */
  ProtonSender setAutoDrained(boolean autoDrained);

  /**
   * Get whether the link will automatically be marked drained after the send queue drain handler fires in drain mode.
   *
   * @return whether the link will automatically be drained after the send queue drain handler fires in drain mode
   * @see #setAutoDrained(boolean)
   */
  boolean isAutoDrained();

  /**
   * Manually mark the link drained, such that if the receiver has requested the link be drained (as indicated by the
   * value of the {@link #getDrain()} flag) then any remaining credit is discarded and if necessary notice sent to the
   * receiver indicating it has been.
   *
   * For use when {@link #isAutoDrained()} is false.
   *
   * @return the number of credits actually discarded
   * @see #setAutoDrained(boolean)
   */
  int drained();
}
