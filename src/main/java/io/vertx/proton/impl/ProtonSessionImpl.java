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
package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonSession;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonLinkOptions;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Record;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonSessionImpl implements ProtonSession {
  private static final Logger LOG = LoggerFactory.getLogger(ProtonSessionImpl.class);

  private final Session session;
  private int autoLinkCounter = 0;
  private Handler<AsyncResult<ProtonSession>> openHandler = (result) -> {
    LOG.trace("Session open completed");
  };
  private Handler<AsyncResult<ProtonSession>> closeHandler = (result) -> {
    if (result.succeeded()) {
      LOG.trace("Session closed");
    } else {
      LOG.warn("Session closed with error", result.cause());
    }
  };

  ProtonSessionImpl(Session session) {
    this.session = session;
    this.session.setContext(this);
    session.setIncomingCapacity(Integer.MAX_VALUE);
  }

  @Override
  public ProtonConnection getConnection() {
    return getConnectionImpl();
  }

  public ProtonConnectionImpl getConnectionImpl() {
    return (ProtonConnectionImpl) this.session.getConnection().getContext();
  }

  public long getOutgoingWindow() {
    return session.getOutgoingWindow();
  }

  @Override
  public ProtonSession setIncomingCapacity(int bytes) {
    session.setIncomingCapacity(bytes);
    return this;
  }

  public int getOutgoingBytes() {
    return session.getOutgoingBytes();
  }

  public EndpointState getRemoteState() {
    return session.getRemoteState();
  }

  public int getIncomingBytes() {
    return session.getIncomingBytes();
  }

  @Override
  public ErrorCondition getRemoteCondition() {
    return session.getRemoteCondition();
  }

  @Override
  public int getIncomingCapacity() {
    return session.getIncomingCapacity();
  }

  public EndpointState getLocalState() {
    return session.getLocalState();
  }

  @Override
  public ProtonSession setCondition(ErrorCondition condition) {
    session.setCondition(condition);
    return this;
  }

  @Override
  public ErrorCondition getCondition() {
    return session.getCondition();
  }

  public void setOutgoingWindow(long outgoingWindowSize) {
    session.setOutgoingWindow(outgoingWindowSize);
  }

  @Override
  public ProtonSessionImpl open() {
    session.open();
    getConnectionImpl().flush();
    return this;
  }

  @Override
  public ProtonSessionImpl close() {
    session.close();
    getConnectionImpl().flush();
    return this;
  }

  @Override
  public ProtonSessionImpl openHandler(Handler<AsyncResult<ProtonSession>> openHandler) {
    this.openHandler = openHandler;
    return this;
  }

  @Override
  public ProtonSessionImpl closeHandler(Handler<AsyncResult<ProtonSession>> closeHandler) {
    this.closeHandler = closeHandler;
    return this;
  }

  private String generateLinkName() {
    // TODO: include useful details in name, like address and container?
    return "auto-" + (autoLinkCounter++);
  }

  private String getOrCreateLinkName(ProtonLinkOptions linkOptions) {
    return linkOptions.getLinkName() == null ? generateLinkName() : linkOptions.getLinkName();
  }

  @Override
  public ProtonReceiver createReceiver(String address) {
    return createReceiver(address, new ProtonLinkOptions());
  }

  @Override
  public ProtonReceiver createReceiver(String address, ProtonLinkOptions receiverOptions) {
    Receiver receiver = session.receiver(getOrCreateLinkName(receiverOptions));

    Symbol[] outcomes = new Symbol[] { Accepted.DESCRIPTOR_SYMBOL, Rejected.DESCRIPTOR_SYMBOL,
        Released.DESCRIPTOR_SYMBOL, Modified.DESCRIPTOR_SYMBOL };

    Source source = new Source();
    source.setAddress(address);
    source.setOutcomes(outcomes);
    source.setDefaultOutcome(Released.getInstance());
    if(receiverOptions.isDynamic()) {
      source.setDynamic(true);
    }

    Target target = new Target();

    receiver.setSource(source);
    receiver.setTarget(target);

    ProtonReceiverImpl r = new ProtonReceiverImpl(receiver);
    r.openHandler((result) -> {
      LOG.trace("Receiver open completed");
    });
    r.closeHandler((result) -> {
      if (result.succeeded()) {
        LOG.trace("Receiver closed");
      } else {
        LOG.warn("Receiver closed with error", result.cause());
      }
    });

    // Default to at-least-once
    r.setQoS(ProtonQoS.AT_LEAST_ONCE);

    return r;
  }

  @Override
  public ProtonSender createSender(String address) {
    return createSender(address, new ProtonLinkOptions());
  }

  @Override
  public ProtonSender createSender(String address, ProtonLinkOptions senderOptions) {
    Sender sender = session.sender(getOrCreateLinkName(senderOptions));

    Symbol[] outcomes = new Symbol[] { Accepted.DESCRIPTOR_SYMBOL, Rejected.DESCRIPTOR_SYMBOL,
        Released.DESCRIPTOR_SYMBOL, Modified.DESCRIPTOR_SYMBOL };
    Source source = new Source();
    source.setOutcomes(outcomes);

    Target target = new Target();
    target.setAddress(address);
    if(senderOptions.isDynamic()) {
      target.setDynamic(true);
    }

    sender.setSource(source);
    sender.setTarget(target);

    ProtonSenderImpl s = new ProtonSenderImpl(sender);
    if (address == null) {
      s.setAnonymousSender(true);
    }

    s.openHandler((result) -> {
      LOG.trace("Sender open completed");
    });
    s.closeHandler((result) -> {
      if (result.succeeded()) {
        LOG.trace("Sender closed");
      } else {
        LOG.warn("Sender closed with error", result.cause());
      }
    });

    // Default to at-least-once
    s.setQoS(ProtonQoS.AT_LEAST_ONCE);

    return s;
  }

  @Override
  public Record attachments() {
    return session.attachments();
  }

  @Override
  public void free() {
    session.free();
    getConnectionImpl().flush();
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // Implementation details hidden from public api.
  //
  /////////////////////////////////////////////////////////////////////////////
  void fireRemoteOpen() {
    if (openHandler != null) {
      openHandler.handle(ProtonHelper.future(this, getRemoteCondition()));
    }
  }

  void fireRemoteClose() {
    if (closeHandler != null) {
      closeHandler.handle(ProtonHelper.future(this, getRemoteCondition()));
    }
  }

}
