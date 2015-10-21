/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.*;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.*;

import java.util.Map;

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

    private Handler<ProtonSessionImpl> sessionOpenHandler = (session) -> {
        session.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };
    private Handler<ProtonSenderImpl> senderOpenHandler = (sender) -> {
        sender.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };
    private Handler<ProtonReceiverImpl> receiverOpenHandler = (receiver) -> {
        receiver.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };

    ProtonConnectionImpl() {
        this.connection.setContext(this);
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

    public ProtonConnectionImpl setCondition(ErrorCondition condition) {
        connection.setCondition(condition);
        return this;
    }

    public ErrorCondition getCondition() {
        return connection.getCondition();
    }

    public String getContainer() {
        return connection.getContainer();
    }

    public String getHostname() {
        return connection.getHostname();
    }

    public EndpointState getLocalState() {
        return connection.getLocalState();
    }

    public ErrorCondition getRemoteCondition() {
        return connection.getRemoteCondition();
    }

    public String getRemoteContainer() {
        return connection.getRemoteContainer();
    }

    public Symbol[] getRemoteDesiredCapabilities() {
        return connection.getRemoteDesiredCapabilities();
    }

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


    public ProtonConnection open() {
        connection.open();
        flush();
        return this;
    }


    public ProtonConnection close() {
        connection.close();
        flush();
        return this;
    }

    public ProtonSessionImpl session() {
        return new ProtonSessionImpl(connection.session());
    }

    public void flush() {
        if (transport != null) {
            transport.flush();
        }
    }

    public void disconnect() {
        if (transport != null) {
            transport.close();
        }
    }

    public ProtonConnection openHandler(Handler<AsyncResult<ProtonConnection>> openHandler) {
        this.openHandler = openHandler;
        return this;
    }
    public ProtonConnection closeHandler(Handler<AsyncResult<ProtonConnection>> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public ProtonConnection sessionOpenHandler(Handler<ProtonSessionImpl> remoteSessionOpenHandler) {
        this.sessionOpenHandler = remoteSessionOpenHandler;
        return this;
    }

    public ProtonConnection senderOpenHandler(Handler<ProtonSenderImpl> remoteSenderOpenHandler) {
        this.senderOpenHandler = remoteSenderOpenHandler;
        return this;
    }

    public ProtonConnection receiverOpenHandler(Handler<ProtonReceiverImpl> remoteReceiverOpenHandler) {
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

    void bind(NetSocket socket) {
        transport = new ProtonTransport(connection, null, socket);
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
