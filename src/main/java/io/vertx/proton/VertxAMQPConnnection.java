/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;

import java.util.ArrayList;
import java.util.Map;

import static io.vertx.proton.VertxAMQPSupport.future;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class VertxAMQPConnnection {

    final Connection connection = Proton.connection();
    VertxAMQPTransport transport;

    private Handler<AsyncResult<VertxAMQPConnnection>> openHandler;
    private Handler<AsyncResult<VertxAMQPConnnection>> closeHandler;

    private Handler<VertxAMQPSession> sessionOpenHandler = (session) -> {
        session.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };
    private Handler<VertxAMQPSender> senderOpenHandler = (sender) -> {
        sender.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };
    private Handler<VertxAMQPReceiver> receiverOpenHandler = (receiver) -> {
        receiver.setCondition(new ErrorCondition(Symbol.getSymbol("Not Supported"), ""));
    };

    VertxAMQPConnnection() {
        this.connection.setContext(this);
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // Delegated state tracking
    //
    /////////////////////////////////////////////////////////////////////////////
    public void setProperties(Map<Symbol, Object> properties) {
        connection.setProperties(properties);
    }

    public void setOfferedCapabilities(Symbol[] capabilities) {
        connection.setOfferedCapabilities(capabilities);
    }

    public void setHostname(String hostname) {
        connection.setHostname(hostname);
    }

    public void setDesiredCapabilities(Symbol[] capabilities) {
        connection.setDesiredCapabilities(capabilities);
    }

    public void setContainer(String container) {
        connection.setContainer(container);
    }

    public void setCondition(ErrorCondition condition) {
        connection.setCondition(condition);
    }

    public ErrorCondition getCondition() {
        return connection.getCondition();
    }

    public String getContainer() {
        return connection.getContainer();
    }

    public Object getContext() {
        return connection.getContext();
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


    public VertxAMQPConnnection open() {
        connection.open();
        return this;
    }


    public VertxAMQPConnnection close() {
        connection.close();
        return this;
    }

    public VertxAMQPSession session() {
        return new VertxAMQPSession(connection.session());
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

    public VertxAMQPConnnection openHandler(Handler<AsyncResult<VertxAMQPConnnection>> openHandler) {
        this.openHandler = openHandler;
        return this;
    }
    public VertxAMQPConnnection closeHandler(Handler<AsyncResult<VertxAMQPConnnection>> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public VertxAMQPConnnection sessionOpenHandler(Handler<VertxAMQPSession> remoteSessionOpenHandler) {
        this.sessionOpenHandler = remoteSessionOpenHandler;
        return this;
    }

    public VertxAMQPConnnection senderOpenHandler(Handler<VertxAMQPSender> remoteSenderOpenHandler) {
        this.senderOpenHandler = remoteSenderOpenHandler;
        return this;
    }

    public VertxAMQPConnnection receiverOpenHandler(Handler<VertxAMQPReceiver> remoteReceiverOpenHandler) {
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
        transport = new VertxAMQPTransport(connection, null, socket);
    }

    void fireRemoteSessionOpen(Session session) {
        if( sessionOpenHandler !=null ) {
            sessionOpenHandler.handle(new VertxAMQPSession(session));
        }
    }

    void fireRemoteLinkOpen(Link link) {
        if( link instanceof Sender ) {
            if( senderOpenHandler !=null ) {
                senderOpenHandler.handle(new VertxAMQPSender((Sender) link));
            }
        } else {
            if( receiverOpenHandler !=null ) {
                receiverOpenHandler.handle(new VertxAMQPReceiver((Receiver) link));
            }
        }
    }
}
