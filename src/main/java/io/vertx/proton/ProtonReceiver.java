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

import java.lang.IllegalStateException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonReceiver extends ProtonLink<ProtonReceiver> {

  /**
   * Sets the handler to process messages as they arrive. Should be set before opening unless prefetch is disabled and
   * credit is being manually controlled.
   *
   * @param handler
   *          the handler to process messages
   * @return the receiver
   */
  ProtonReceiver handler(ProtonMessageHandler handler);

  /**
   * Sets the number of message credits the receiver grants and replenishes automatically as messages are delivered.
   *
   * To manage credit manually, you can instead set prefetch to 0 before opening the consumer and then explicitly call
   * {@link #flow(int)} as needed to manually grant credit.
   *
   * @param messages
   *          the message prefetch
   * @return the receiver
   */
  ProtonReceiver setPrefetch(int messages);

  /**
   * Get the current prefetch value.
   *
   * @return the prefetch
   * @see #setPrefetch(int)
   */
  int getPrefetch();

  /**
   * Sets whether received deliveries should be automatically accepted (and settled) after the message handler runs for
   * them, if no other disposition has been applied during handling.
   *
   * True by default.
   *
   * @param autoAccept
   *          whether deliveries should be auto accepted after handling if no disposition was applied
   * @return the receiver
   */
  ProtonReceiver setAutoAccept(boolean autoAccept);

  /**
   * Get whether the receiver is auto accepting.
   *
   * @return whether deliveries are being auto accepted after handling if no disposition was applied
   * @see #setAutoAccept(boolean)
   */
  boolean isAutoAccept();

  /**
   * Grants the given number of message credits to the sender.
   *
   * For use when {@link #setPrefetch(int)} has been used to disable automatic prefetch credit handling.
   *
   * @param credits
   *          the credits to flow
   * @return the receiver
   * @throws IllegalStateException
   *           if prefetch is non-zero, or an existing drain operation is not yet complete
   */
  ProtonReceiver flow(int credits) throws IllegalStateException;

  /**
   * Initiates a 'drain' of link credit from the remote sender.
   *
   * The timeout parameter allows scheduling a delay (in milliseconds) after which the handler should be fired with
   * a failure result if the attempt has not yet completed successfully, with a value of 0 equivalent to no-timeout.
   *
   * If a drain attempt fails due to timeout, it is no longer possible to reason about the 'drain' state of the receiver
   * and thus any further attempts to drain it should be avoided. The receiver should typically be closed in such cases.
   *
   * Only available for use when {@link #setPrefetch(int)} has been used to disable automatic credit handling.
   *
   * @param timeout
   *          the delay in milliseconds before which the drain attempt should be considered failed, or 0 for no timeout.
   * @param completionHandler
   *          handler called when credit hits 0 due to messages arriving, or a 'drain response' flow
   *
   * @return the receiver
   * @throws IllegalStateException
   *           if prefetch is non-zero, or an existing drain operation is not yet complete
   * @throws IllegalArgumentException
   *           if no completion handler is given
   */
  ProtonReceiver drain(long timeout, Handler<AsyncResult<Void>> completionHandler) throws IllegalStateException, IllegalArgumentException;
}
