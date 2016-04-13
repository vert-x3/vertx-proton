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

import org.apache.qpid.proton.amqp.transport.DeliveryState;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonDelivery {

  /**
   * Updates the DeliveryState, and optionally settle the delivery as well.
   *
   * @param state
   *          the delivery state to apply
   * @param settle
   *          whether to {@link #settle()} the delivery at the same time
   * @return itself
   */
  ProtonDelivery disposition(DeliveryState state, boolean settle);

  /**
   * Gets the current local state for the delivery.
   *
   * @return the delivery state
   */
  DeliveryState getLocalState();

  /**
   * Gets the current remote state for the delivery.
   *
   * @return the remote delivery state
   */
  DeliveryState getRemoteState();

  /**
   * Settles the delivery locally.
   *
   * @return the delivery
   */
  ProtonDelivery settle();

  /**
   * Gets whether the delivery was settled by the remote peer yet.
   *
   * @return whether the delivery is remotely settled
   */
  boolean remotelySettled();

  /**
   * Gets the delivery tag for this delivery
   *
   * @return the tag
   */
  byte[] getTag();

  /**
   * Gets the message format for the current delivery.
   *
   * @return the message format
   */
  int getMessageFormat();
}
