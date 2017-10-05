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

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedLong;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Record;

import java.util.Map;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonLink<T extends ProtonLink<T>> {

  /**
   * Opens the AMQP link, i.e. allows the Attach frame to be emitted. Typically used after any additional configuration
   * is performed on the object.
   *
   * For locally initiated links, the {@link #openHandler(Handler)} may be used to handle the peer sending their Attach
   * frame.
   *
   * @return the link
   */
  T open();

  /**
   * Closes the AMQP link, i.e. allows the Detach frame to be emitted with closed=true set.
   *
   * If the closure is being locally initiated, the {@link #closeHandler(Handler)} should be used to handle the peer
   * sending their Detach frame with closed=true (and {@link #detachHandler(Handler)} can be used to handle the peer
   * sending their Detach frame with closed=false).
   *
   * @return the link
   */
  T close();

  /**
   * Detaches the AMQP link, i.e. allows the Detach frame to be emitted with closed=false.
   *
   * If the detach is being locally initiated, the {@link #detachHandler(Handler)} should be used to handle the peer
   * sending their Detach frame with closed=false (and {@link #closeHandler(Handler)} can be used to handle the peer
   * sending their Detach frame with closed=true).
   *
   * @return the link
   */
  T detach();

  /**
   * Sets a handler for when an AMQP Attach frame is received from the remote peer.
   *
   * Typically used by clients, servers rely on {@link ProtonConnection#senderOpenHandler(Handler)} and
   * {@link ProtonConnection#receiverOpenHandler(Handler)}.
   *
   * @param remoteOpenHandler
   *          the handler
   * @return the link
   */
  T openHandler(Handler<AsyncResult<T>> remoteOpenHandler);

  /**
   * Sets a handler for when an AMQP Detach frame with closed=true is received from the remote peer.
   *
   * @param remoteCloseHandler
   *          the handler
   * @return the link
   */
  T closeHandler(Handler<AsyncResult<T>> remoteCloseHandler);

  /**
   * Sets a handler for when an AMQP Detach frame with closed=false is received from the remote peer.
   *
   * @param remoteDetachHandler
   *          the handler
   * @return the link
   */
  T detachHandler(Handler<AsyncResult<T>> remoteDetachHandler);

  /**
   * Gets the local QOS config.
   *
   * @return the QOS config
   */
  ProtonQoS getQoS();

  /**
   * Sets the local QOS config.
   *
   * @param qos
   *          the QOS to configure
   * @return the link
   */
  T setQoS(ProtonQoS qos);

  /**
   * Gets the remote QOS config.
   *
   * @return the QOS config
   */
  ProtonQoS getRemoteQoS();

  /**
   * Check whether the link is locally open.
   *
   * @return whether the link is locally open.
   */
  boolean isOpen();

  /**
   * Retrieves the attachments record, upon which application items can be set/retrieved.
   *
   * @return the attachments
   */
  Record attachments();

  /**
   * Gets the current local target config.
   *
   * @return the target
   */
  Target getTarget();

  /**
   * Sets the current local target config. Only useful to call before the link has locally opened.
   *
   * @param target
   *          the target
   * @return the link
   */
  T setTarget(Target target);

  /**
   * Gets the current remote target config. Only useful to call after the link has remotely opened.
   *
   * @return the target
   */
  Target getRemoteTarget();

  /**
   * Gets the current local source config.
   *
   * @return the source
   */
  Source getSource();

  /**
   * Sets the current local source config. Only useful to call before the link has locally opened.
   *
   * @param source
   *          the source
   * @return the link
   */
  T setSource(Source source);

  /**
   * Gets the current remote source config. Only useful to call after the link has remotely opened.
   *
   * @return the target
   */
  Source getRemoteSource();

  /**
   * Gets the session this link is on.
   * @return the session
   */
  ProtonSession getSession();

  /**
   * Sets the local ErrorCondition object.
   *
   * @param condition
   *          the condition to set
   * @return the link
   */
  T setCondition(ErrorCondition condition);

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
   * Retrieves the current amount of credit.
   *
   * For a receiver link, the value returned will still include the credits that will be used by any queued
   * incoming messages, use {@link #getQueued()} to assess the number of credits that will be used by queued messages.
   *
   * @return the number of credits
   */
  int getCredit();

  /**
   * Retrieves the current value of link 'drain' flag.
   *
   * @return when the link drain flag is set.
   */
  boolean getDrain();

  /**
   * Retrieves the current number of queued messages.
   *
   * For a receiver link, this is the number of messages that have already arrived locally but not yet been processed.
   *
   * @return the number of queues messages
   */
  int getQueued();

  /**
   * Retrieves the link name
   *
   * @return  the link name
   */
  String getName();

  /**
   * Sets the local link max message size, to be conveyed to the peer via the Attach frame
   * when attaching the link to the session. Null or 0 means no limit.
   *
   * Must be called during link setup, i.e. before calling the {@link #open()} method.
   *
   * @param maxMessageSize
   *            the local max message size value, or null to clear. 0 also means no limit.
   */
  void setMaxMessageSize(UnsignedLong maxMessageSize);

  /**
   * Gets the local link max message size.
   *
   * @return the local max message size, or null if none was set. 0 also means no limit.
   *
   * @see #setMaxMessageSize(UnsignedLong)
   */
  UnsignedLong getMaxMessageSize();

  /**
   * Gets the remote link max message size, as conveyed from the peer via the Attach frame
   * when attaching the link to the session.
   *
   * @return the remote max message size conveyed by the peer, or null if none was set. 0 also means no limit.
   */
  UnsignedLong getRemoteMaxMessageSize();


  /**
   * Sets the link properties, to be conveyed to the peer via the Attach frame
   * when attaching the link to the session.
   *
   * Must be called during link setup, i.e. before calling the {@link #open()} method.
   *
   * @param properties the properties of the link to be coveyed to the remote peer.
   */
  void setProperties(Map<Symbol, Object> properties);

  /**
   * Gets the remote link properties, as conveyed from the peer via the Attach frame
   * when attaching the link to the session.
   *
   * @return the remote link properties conveyed by the peer, or null if none was set.
   */
  Map<Symbol, Object> getRemoteProperties();


  /**
   * Sets the offered capabilities, to be conveyed to the peer via the Attach frame
   * when attaching the link to the session.
   *
   * Must be called during link setup, i.e. before calling the {@link #open()} method.
   *
   * @param capabilities the capabilities offered to the remote peer.
   */
  void setOfferedCapabilities(Symbol[] capabilities);

  /**
   * Gets the remote offered capabilities, as conveyed from the peer via the Attach frame
   * when attaching the link to the session.
   *
   * @return the remote offered capabilities conveyed by the peer, or null if none was set.
   */
  Symbol[] getRemoteOfferedCapabilities();

  /**
   * Sets the desired capabilities, to be conveyed to the peer via the Attach frame
   * when attaching the link to the session.
   *
   * Must be called during link setup, i.e. before calling the {@link #open()} method.
   *
   * @param capabilities the capabilities desired of the remote peer.
   */
  void setDesiredCapabilities(Symbol[] capabilities);

  /**
   * Gets the remote desired capabilities, as conveyed from the peer via the Attach frame
   * when attaching the link to the session.
   *
   * @return the remote desired capabilities conveyed by the peer, or null if none was set.
   */
  Symbol[] getRemoteDesiredCapabilities();

}
