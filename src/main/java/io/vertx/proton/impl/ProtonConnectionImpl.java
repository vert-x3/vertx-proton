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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.ProtonSession;
import io.vertx.proton.ProtonTransportOptions;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Record;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.vertx.proton.ProtonHelper.future;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonConnectionImpl implements ProtonConnection {
  private static final Logger LOG = LoggerFactory.getLogger(ProtonConnectionImpl.class);

  public static final Symbol ANONYMOUS_RELAY = Symbol.valueOf("ANONYMOUS-RELAY");

  private final Connection connection = Proton.connection();
  private final Vertx vertx;
  private final ContextInternal connCtx;
  private ProtonTransport transport;
  private List<Handler<Void>> endHandlers = new ArrayList<>();

  private Handler<AsyncResult<ProtonConnection>> openHandler = (result) -> {
    LOG.trace("Connection open completed");
  };
  private Handler<AsyncResult<ProtonConnection>> closeHandler = (result) -> {
    if (result.succeeded()) {
      LOG.trace("Connection closed");
    } else {
      LOG.warn("Connection closed with error", result.cause());
    }
  };
  private Handler<ProtonConnection> disconnectHandler = (connection) -> {
    LOG.trace("Connection disconnected");
  };

  private Handler<ProtonSession> sessionOpenHandler = (session) -> {
    session.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
  };
  private Handler<ProtonSender> senderOpenHandler = (sender) -> {
    sender.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
  };
  private Handler<ProtonReceiver> receiverOpenHandler = (receiver) -> {
    receiver.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
  };
  private boolean anonymousRelaySupported;
  private ProtonSession defaultSession;

  ProtonConnectionImpl(Vertx vertx, String hostname, ContextInternal connCtx) {
    this.vertx = vertx;
    this.connCtx = connCtx;
    this.connection.setContext(this);
    this.connection.setContainer("vert.x-" + UUID.randomUUID());
    this.connection.setHostname(hostname);

    Map<Symbol, Object> props = createInitialPropertiesMap();
    connection.setProperties(props);
  }

  private LinkedHashMap<Symbol, Object> createInitialPropertiesMap() {
    LinkedHashMap<Symbol, Object> props = new LinkedHashMap<Symbol, Object>();
    props.put(ProtonMetaDataSupportImpl.PRODUCT_KEY, ProtonMetaDataSupportImpl.PRODUCT);
    props.put(ProtonMetaDataSupportImpl.VERSION_KEY, ProtonMetaDataSupportImpl.VERSION);
    return props;
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // Delegated state tracking
  //
  /////////////////////////////////////////////////////////////////////////////

  @Override
  public ProtonConnectionImpl setProperties(final Map<Symbol, Object> properties) {
    LinkedHashMap<Symbol, Object> newProps = null;
    if (properties != null) {
      newProps = createInitialPropertiesMap();
      newProps.putAll(properties);
    }

    connection.setProperties(newProps);
    return this;
  }

  @Override
  public ProtonConnectionImpl setOfferedCapabilities(Symbol[] capabilities) {
    connection.setOfferedCapabilities(capabilities);
    return this;
  }

  @Override
  public ProtonConnectionImpl setHostname(String hostname) {
    connection.setHostname(hostname);
    return this;
  }

  @Override
  public ProtonConnectionImpl setDesiredCapabilities(Symbol[] capabilities) {
    connection.setDesiredCapabilities(capabilities);
    return this;
  }

  @Override
  public ProtonConnectionImpl setContainer(String container) {
    connection.setContainer(container);
    return this;
  }

  @Override
  public ProtonConnectionImpl setCondition(ErrorCondition condition) {
    connection.setCondition(condition);
    return this;
  }

  @Override
  public ErrorCondition getCondition() {
    return connection.getCondition();
  }

  @Override
  public String getContainer() {
    return connection.getContainer();
  }

  @Override
  public String getHostname() {
    return connection.getHostname();
  }

  public EndpointState getLocalState() {
    return connection.getLocalState();
  }

  @Override
  public ErrorCondition getRemoteCondition() {
    return connection.getRemoteCondition();
  }

  @Override
  public String getRemoteContainer() {
    return connection.getRemoteContainer();
  }

  @Override
  public Symbol[] getRemoteDesiredCapabilities() {
    return connection.getRemoteDesiredCapabilities();
  }

  @Override
  public String getRemoteHostname() {
    return connection.getRemoteHostname();
  }

  @Override
  public Symbol[] getRemoteOfferedCapabilities() {
    return connection.getRemoteOfferedCapabilities();
  }

  @Override
  public Map<Symbol, Object> getRemoteProperties() {
    return connection.getRemoteProperties();
  }

  public EndpointState getRemoteState() {
    return connection.getRemoteState();
  }

  @Override
  public boolean isAnonymousRelaySupported() {
    return anonymousRelaySupported;
  };

  @Override
  public Record attachments() {
    return connection.attachments();
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // Handle/Trigger connection level state changes
  //
  /////////////////////////////////////////////////////////////////////////////

  @Override
  public ProtonConnection open() {
    connection.open();
    flush();
    return this;
  }

  @Override
  public ProtonConnection close() {
    connection.close();
    flush();
    return this;
  }

  @Override
  public ProtonSessionImpl createSession() {
    return new ProtonSessionImpl(connection.session());
  }

  private ProtonSession getDefaultSession() {
    if (defaultSession == null) {
      defaultSession = createSession();
      defaultSession.closeHandler(result -> {
        String msg = "The connections default session closed unexpectedly";
        if (!result.succeeded()) {
          msg += ": ";
          msg += ": " + String.valueOf(result.cause());
        }
        Future<ProtonConnection> failure = Future.failedFuture(msg);
        Handler<AsyncResult<ProtonConnection>> connCloseHandler = closeHandler;
        if (connCloseHandler != null) {
          connCloseHandler.handle(failure);
        }
      });

      defaultSession.open();
      // Deliberately not flushing, the sender/receiver open
      // call will do that (if it doesn't happen otherwise).
    }
    return defaultSession;
  }

  @Override
  public ProtonSender createSender(String address) {
    return getDefaultSession().createSender(address);
  }

  @Override
  public ProtonSender createSender(String address, ProtonLinkOptions senderOptions) {
      return getDefaultSession().createSender(address, senderOptions);
  }

  @Override
  public ProtonReceiver createReceiver(String address) {
    return getDefaultSession().createReceiver(address);
  }

  @Override
  public ProtonReceiver createReceiver(String address, ProtonLinkOptions receiverOptions) {
      return getDefaultSession().createReceiver(address, receiverOptions);
  }

  public void flush() {
    if (transport != null) {
      transport.flush();
    }
  }

  @Override
  public void disconnect() {
    if (transport != null) {
      transport.disconnect();
    }
  }

  @Override
  public boolean isDisconnected() {
    return transport == null;
  }

  @Override
  public ProtonConnection openHandler(Handler<AsyncResult<ProtonConnection>> openHandler) {
    this.openHandler = openHandler;
    return this;
  }

  @Override
  public ProtonConnection closeHandler(Handler<AsyncResult<ProtonConnection>> closeHandler) {
    this.closeHandler = closeHandler;
    return this;
  }

  @Override
  public ProtonConnection disconnectHandler(Handler<ProtonConnection> disconnectHandler) {
    this.disconnectHandler = disconnectHandler;
    return this;
  }

  @Override
  public ProtonConnection sessionOpenHandler(Handler<ProtonSession> remoteSessionOpenHandler) {
    this.sessionOpenHandler = remoteSessionOpenHandler;
    return this;
  }

  @Override
  public ProtonConnection senderOpenHandler(Handler<ProtonSender> remoteSenderOpenHandler) {
    this.senderOpenHandler = remoteSenderOpenHandler;
    return this;
  }

  @Override
  public ProtonConnection receiverOpenHandler(Handler<ProtonReceiver> remoteReceiverOpenHandler) {
    this.receiverOpenHandler = remoteReceiverOpenHandler;
    return this;
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // Implementation details hidden from public api.
  //
  /////////////////////////////////////////////////////////////////////////////

  private void processCapabilities() {
    Symbol[] capabilities = getRemoteOfferedCapabilities();
    if (capabilities != null) {
      List<Symbol> list = Arrays.asList(capabilities);
      if (list.contains(ANONYMOUS_RELAY)) {
        anonymousRelaySupported = true;
      }
    }
  }

  void fireRemoteOpen() {
    processCapabilities();

    if (openHandler != null) {
      openHandler.handle(future(this, getRemoteCondition()));
    }
  }

  void fireRemoteClose() {
    if (closeHandler != null) {
      closeHandler.handle(future(this, getRemoteCondition()));
    }
  }

  public void fireDisconnect() {
    transport = null;
    if (disconnectHandler != null) {
      disconnectHandler.handle(this);
    }

    Iterator<Handler<Void>> iter = endHandlers.iterator();
    while(iter.hasNext()) {
      Handler<Void> h = iter.next();
      iter.remove();
      h.handle(null);
    }
  }

  void bindClient(NetClient client, NetSocket socket, ProtonSaslClientAuthenticatorImpl authenticator, ProtonTransportOptions transportOptions) {
    transport = new ProtonTransport(connection, vertx, client, socket, authenticator, transportOptions);
  }

  void bindServer(NetSocket socket, ProtonSaslAuthenticator authenticator, ProtonTransportOptions transportOptions) {
    transport = new ProtonTransport(connection, vertx, null, socket, authenticator, transportOptions);
  }

  void fireRemoteSessionOpen(Session session) {
    if (sessionOpenHandler != null) {
      sessionOpenHandler.handle(new ProtonSessionImpl(session));
    }
  }

  void fireRemoteLinkOpen(Link link) {
    if (link instanceof Sender) {
      if (senderOpenHandler != null) {
        senderOpenHandler.handle(new ProtonSenderImpl((Sender) link));
      }
    } else {
      if (receiverOpenHandler != null) {
        receiverOpenHandler.handle(new ProtonReceiverImpl((Receiver) link));
      }
    }
  }

  public void addEndHandler(Handler<Void> handler) {
    endHandlers.add(handler);
  }

  public ContextInternal getContext() {
    return connCtx;
  }
}
