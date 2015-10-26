/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.*;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.message.Message;

import java.util.Map;
import java.util.UUID;

import static io.vertx.proton.ProtonHelper.future;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonConnectionImpl implements ProtonConnection {

    final Connection connection = Proton.connection();
    ProtonTransport transport;

    private Handler<AsyncResult<ProtonConnection>> openHandler;
    private Handler<AsyncResult<ProtonConnection>> closeHandler;
    private Handler<ProtonConnection> disconnectHandler;

    private Handler<ProtonSession> sessionOpenHandler = (session) -> {
        session.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };
    private Handler<ProtonSender> senderOpenHandler = (sender) -> {
        sender.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };
    private Handler<ProtonReceiver> receiverOpenHandler = (receiver) -> {
        receiver.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };
    private ProtonSession defaultSession;
    private ProtonSender defaultSender;
    private ProtonReceiver defaultReceiver;

    ProtonConnectionImpl(String hostname) {
        this.connection.setContext(this);
        this.connection.setContainer("vert.x-"+UUID.randomUUID());
        this.connection.setHostname(hostname);
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // Delegated state tracking
    //
    /////////////////////////////////////////////////////////////////////////////
    public ProtonConnectionImpl setProperties(Map<Symbol, Object> properties) {
        connection.setProperties(properties);
        return this;
    }

    public ProtonConnectionImpl setOfferedCapabilities(Symbol[] capabilities) {
        connection.setOfferedCapabilities(capabilities);
        return this;
    }

    @Override
    public ProtonConnectionImpl setHostname(String hostname) {
        connection.setHostname(hostname);
        return this;
    }

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

    public Symbol[] getRemoteDesiredCapabilities() {
        return connection.getRemoteDesiredCapabilities();
    }

    @Override
    public String getRemoteHostname() {
        return connection.getRemoteHostname();
    }

    public Symbol[] getRemoteOfferedCapabilities() {
        return connection.getRemoteOfferedCapabilities();
    }

    public Map<Symbol, Object> getRemoteProperties() {
        return connection.getRemoteProperties();
    }

    public EndpointState getRemoteState() {
        return connection.getRemoteState();
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
    public ProtonSessionImpl session() {
        return new ProtonSessionImpl(connection.session());
    }

    public ProtonSession getDefaultSession() {
        if( defaultSession == null ) {
            defaultSession = new ProtonSessionImpl(connection.session());
            defaultSession.open();
        }
        return defaultSession;
    }

    public ProtonSender getDefaultSender() {
        if( defaultSender == null ) {
            defaultSender = getDefaultSession().sender();
            defaultSender.open();
        }
        return defaultSender;
    }

    @Override
    public void send(byte[] tag, Message message) {
        getDefaultSender().send(tag, message);
    }

    @Override
    public void send(byte[] tag, Message message, Handler<ProtonDelivery> onReceived) {
        getDefaultSender().send(tag, message, onReceived);
    }

    @Override
    public ProtonReceiver receiver(String name) {
        return getDefaultSession().receiver(name);
    }

    @Override
    public ProtonReceiver receiver() {
        return getDefaultSession().receiver();
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
        return transport==null;
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
    void fireRemoteOpen() {
        if( openHandler !=null ) {
            openHandler.handle(future(this, getRemoteCondition()));
        }
    }

    void fireRemoteClose() {
        if( closeHandler !=null ) {
            closeHandler.handle(future(this, getRemoteCondition()));
        }
    }

    public void fireDisconnect() {
        transport = null;
        if( disconnectHandler !=null ) {
            disconnectHandler.handle(this);
        }
    }

    void bind(NetClient client, NetSocket socket) {
        transport = new ProtonTransport(connection, client, socket);
    }

    void bind(NetSocket socket) {
        bind(null, socket);
    }

    void fireRemoteSessionOpen(Session session) {
        if( sessionOpenHandler !=null ) {
            sessionOpenHandler.handle(new ProtonSessionImpl(session));
        }
    }

    void fireRemoteLinkOpen(Link link) {
        if( link instanceof Sender ) {
            if( senderOpenHandler !=null ) {
                senderOpenHandler.handle(new ProtonSenderImpl((Sender) link));
            }
        } else {
            if( receiverOpenHandler !=null ) {
                receiverOpenHandler.handle(new ProtonReceiverImpl((Receiver) link));
            }
        }
    }

}
