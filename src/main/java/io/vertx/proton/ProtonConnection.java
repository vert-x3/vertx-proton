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

import java.util.Map;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Record;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface ProtonConnection {

  /**
   * Opens the AMQP connection, i.e. allows the Open frame to be emitted. Typically used after any additional
   * configuration is performed on the connection object.
   *
   * For locally initiated connections, the {@link #openHandler(Handler)} may be used to handle the peer sending their
   * Open frame.
   *
   * @return the connection
   */
  ProtonConnection open();

  /**
   * Closes the AMQP connection, i.e. allows the Close frame to be emitted.
   *
   * For locally initiated connections, the {@link #closeHandler(Handler)} may be used to handle the peer sending their
   * Close frame (if they haven't already).
   *
   * @return the connection
   */
  ProtonConnection close();

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
   * Sets the container id value advertised to peers in the AMQP Open frame. Sometimes used as a 'client-id' by clients.
   *
   * @param container
   *          the container id to set
   * @return the connection
   */
  ProtonConnection setContainer(String container);

  /**
   * Gets the container id value requested of/advertised to peers in the AMQP Open frame.
   *
   * @return the container id
   */
  String getContainer();

  /**
   * Retrieves the attachments record, upon which application items can be set/retrieved.
   *
   * @return the attachments
   */
  Record attachments();

  /**
   * Sets the connection properties map to be sent to the remote peer in our Open frame.
   *
   * If non-null, the given map will be copied and augmented with the default map containing "product" and "version"
   * entries if not present in the given properties. If null, no properties map will be sent.
   *
   * @param properties
   *          the properties map, or null to request not sending any properties map
   * @return the connection
   */
  ProtonConnection setProperties(Map<Symbol, Object> properties);

  /**
   * Returns the connection properties map if sent by the remote peer in its Open frame. May be null.
   *
   * @return the remote connection properties map, or null if no map was sent.
   */
  Map<Symbol, Object> getRemoteProperties();

  /**
   * Allows querying (once the connection has remotely opened) whether the peer advertises support for the anonymous
   * relay (sender with null address).
   *
   * @return true if the peer advertised support for the anonymous relay
   */
  boolean isAnonymousRelaySupported();

  /**
   * Creates a new session, which can be used to create new senders/receivers on.
   *
   * @return the (unopened) session.
   */
  ProtonSession createSession();

  /**
   * Disconnects the underlying transport connection.
   */
  void disconnect();

  /**
   * Gets whether the underlying transport is disconnected.
   *
   * @return whether the underlying transport is disconnected.
   */
  boolean isDisconnected();

  /**
   * Sets the hostname value requested of/advertised to peers in the AMQP Open frame.
   *
   * @param hostname
   *          the hostname to set
   * @return the connection
   */
  ProtonConnection setHostname(String hostname);

  /**
   * Gets the hostname value requested of/advertised to peers in the AMQP Open frame.
   *
   * @return the hostname
   */
  String getHostname();

  /**
   * Returns the container value requested by/advertised by remote peer in their AMQP Open frame.
   *
   * @return the container id
   */
  String getRemoteContainer();

  /**
   * Returns the container value requested by/advertised by remote peer in their AMQP Open frame.
   *
   * @return the container id
   */
  String getRemoteHostname();

  /**
   * Sets the local ErrorCondition object.
   *
   * @param condition
   *          the condition to set
   * @return the connection
   */
  ProtonConnection setCondition(ErrorCondition condition);

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
   * Sets a handler for when an AMQP Open frame is received from the remote peer.
   *
   * @param remoteOpenHandler
   *          the handler
   * @return the connection
   */
  ProtonConnection openHandler(Handler<AsyncResult<ProtonConnection>> remoteOpenHandler);

  /**
   * Sets a handler for when an AMQP Close frame is received from the remote peer.
   *
   * @param remoteCloseHandler
   *          the handler
   * @return the connection
   */
  ProtonConnection closeHandler(Handler<AsyncResult<ProtonConnection>> remoteCloseHandler);

  /**
   * Sets a handler for when an AMQP Begin frame is received from the remote peer.
   *
   * Used to process remotely initiated Sessions. Locally initiated sessions have their own handler invoked instead.
   * Typically used by servers.
   *
   * @param remoteSessionOpenHandler
   *          the handler
   * @return the connection
   */
  ProtonConnection sessionOpenHandler(Handler<ProtonSession> remoteSessionOpenHandler);

  /**
   * Sets a handler for when an AMQP Attach frame is received from the remote peer for a sending link.
   *
   * Used to process remotely initiated sending link. Locally initiated links have their own handler invoked instead.
   * Typically used by servers.
   *
   * @param remoteSenderOpenHandler
   *          the handler
   * @return the connection
   */
  ProtonConnection senderOpenHandler(Handler<ProtonSender> remoteSenderOpenHandler);

  /**
   * Sets a handler for when an AMQP Attach frame is received from the remote peer for a receiving link.
   *
   * Used to process remotely initiated receiving link. Locally initiated links have their own handler invoked instead.
   * Typically used by servers.
   *
   * @param remoteReceiverOpenHandler
   *          the handler
   * @return the connection
   */
  ProtonConnection receiverOpenHandler(Handler<ProtonReceiver> remoteReceiverOpenHandler);

  /**
   * Sets a handler for when the underlying transport connection disconnects.
   *
   * @return the connection
   * @param disconnectHandler
   *          the handler
   */
  ProtonConnection disconnectHandler(Handler<ProtonConnection> disconnectHandler);

}
