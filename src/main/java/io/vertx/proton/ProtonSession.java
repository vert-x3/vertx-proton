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
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Record;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonSession {

  /**
   * Creates a receiver used to consumer messages from the given node address.
   *
   * @param address
   *          The source address to attach the consumer to.
   *
   * @return the (unopened) consumer.
   */
  ProtonReceiver createReceiver(String address);

  /**
   * Creates a sender used to send messages to the given node address. If no address (i.e null) is specified then a
   * sender will be established to the 'anonymous relay' and each message must specify its destination address.
   *
   * @param address
   *          The target address to attach to, or null to attach to the anonymous relay.
   *
   * @return the (unopened) sender.
   */
  ProtonSender createSender(String address);

  /**
   * Opens the AMQP session, i.e. allows the Begin frame to be emitted. Typically used after any additional
   * configuration is performed on the object.
   *
   * For locally initiated sessions, the {@link #openHandler(Handler)} may be used to handle the peer sending their
   * Begin frame.
   *
   * @return the session
   */
  ProtonSession open();

  /**
   * Closed the AMQP session, i.e. allows the End frame to be emitted.
   *
   * If the closure is being locally initiated, the {@link #closeHandler(Handler)} may be used to handle the peer
   * sending their End frame.
   *
   * @return the session
   */
  ProtonSession close();

  /**
   * Retrieves the attachments record, upon which application items can be set/retrieved.
   *
   * @return the attachments
   */
  Record attachments();

  /**
   * Sets the incoming capacity in bytes, used to govern session-level flow control.
   *
   * @param capacity
   *          capacity in bytes
   * @return the session
   */
  ProtonSession setIncomingCapacity(int capacity);

  /**
   * Gets the incoming capacity in bytes, used to govern session-level flow control.
   *
   * @return capacity in bytes
   */
  int getIncomingCapacity();

  /**
   * Sets the local ErrorCondition object.
   *
   * @param condition
   *          the condition to set
   * @return the session
   */
  ProtonSession setCondition(ErrorCondition condition);

  /**
   * Gets the local ErrorCondition object.
   *
   * @return the condition
   */
  ErrorCondition getCondition();

  /**
   * Gets the remote ErrorCondition object.
   *
   * @return the condition
   */
  ErrorCondition getRemoteCondition();

  /**
   * Sets a handler for when an AMQP Begin frame is received from the remote peer.
   *
   * Typically used by clients, servers rely on {@link ProtonConnection#sessionOpenHandler(Handler)}.
   *
   * @param remoteOpenHandler
   *          the handler
   * @return the session
   */
  ProtonSession openHandler(Handler<AsyncResult<ProtonSession>> remoteOpenHandler);

  /**
   * Sets a handler for when an AMQP End frame is received from the remote peer.
   *
   * @param remoteCloseHandler
   *          the handler
   * @return the session
   */
  ProtonSession closeHandler(Handler<AsyncResult<ProtonSession>> remoteCloseHandler);
}
