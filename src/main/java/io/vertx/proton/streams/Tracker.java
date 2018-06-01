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
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.message.Message;

import io.vertx.core.Handler;
import io.vertx.proton.streams.impl.TrackerImpl;

public interface Tracker {

  /**
   * Creates a tracker for sending a message.
   *
   * @param message
   *          the message
   * @return the tracker
   */
  static Tracker create(Message message) {
    return new TrackerImpl(message, null);
  }

  /**
   * Creates a tracker for sending a message, providing a handler that is
   * called when disposition updates are received for the message delivery.
   *
   * @param message
   *          the message
   * @param onUpdated
   *          the onUpdated handler
   * @return the tracker
   */
  static Tracker create(Message message, Handler<Tracker> onUpdated) {
    return new TrackerImpl(message, onUpdated);
  }

  // ===========================

  /**
   * Retrieves the message being tracked.
   *
   * @return the message
   */
  Message message();

  /**
   * Returns whether the message was accepted.
   *
   * Equivalent to: tracker.getRemoteState() instanceof Accepted;
   *
   * @return true if the message was accepted.
   */
  boolean isAccepted();

  /**
   * Returns whether the message was settled by the peer.
   *
   * @return true if the message was settled.
   */
  boolean isRemotelySettled();

  /**
   * Gets the current remote state for the delivery.
   *
   * @return the remote delivery state
   */
  DeliveryState getRemoteState();
}
