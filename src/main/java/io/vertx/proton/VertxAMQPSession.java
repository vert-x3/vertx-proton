/**
 * Copyright 2015 Red Hat, Inc.
 */

package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Session;

import static io.vertx.proton.VertxAMQPSupport.future;
import static io.vertx.proton.VertxAMQPSupport.source;
import static io.vertx.proton.VertxAMQPSupport.target;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class VertxAMQPSession {

    private final Session session;
    private Handler<AsyncResult<VertxAMQPSession>> openHandler;
    private Handler<AsyncResult<VertxAMQPSession>> closeHandler;

    VertxAMQPSession(Session session) {
        this.session = session;
        this.session.setContext(this);
    }

    public VertxAMQPConnnection getLink() {
                return (VertxAMQPConnnection) this.session.getConnection().getContext();
            }

    public long getOutgoingWindow() {
        return session.getOutgoingWindow();
    }

    public void setIncomingCapacity(int bytes) {
        session.setIncomingCapacity(bytes);
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

    public ErrorCondition getRemoteCondition() {
        return session.getRemoteCondition();
    }

    public int getIncomingCapacity() {
        return session.getIncomingCapacity();
    }

    public EndpointState getLocalState() {
        return session.getLocalState();
    }

    public void setCondition(ErrorCondition condition) {
        session.setCondition(condition);
    }

    public ErrorCondition getCondition() {
        return session.getCondition();
    }

    public void setOutgoingWindow(long outgoingWindowSize) {
        session.setOutgoingWindow(outgoingWindowSize);
    }


    public VertxAMQPSession open() {
        session.open();
        return this;
    }

    public VertxAMQPSession close() {
        session.close();
        return this;
    }

    public VertxAMQPSession openHandler(Handler<AsyncResult<VertxAMQPSession>> openHandler) {
        this.openHandler = openHandler;
        return this;
    }

    public VertxAMQPSession closeHandler(Handler<AsyncResult<VertxAMQPSession>> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public VertxAMQPSender sender(String name) {
        return new VertxAMQPSender(session.sender(name))
                .setSenderSettleMode(SenderSettleMode.UNSETTLED)
                .setReceiverSettleMode(ReceiverSettleMode.FIRST);

    }

    public VertxAMQPSender sender(String name, String address) {
        return sender(name).setTarget(target(address));
    }

    public VertxAMQPReceiver receiver(String name) {
        return new VertxAMQPReceiver(session.receiver(name))
                .setSenderSettleMode(SenderSettleMode.UNSETTLED)
                .setReceiverSettleMode(ReceiverSettleMode.FIRST);

    }

    public VertxAMQPReceiver receiver(String name, String address) {
        return receiver(name).setSource(source(address));
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    // Implementation details hidden from public api.
    //
    /////////////////////////////////////////////////////////////////////////////
    void fireRemoteOpen() {
        if (openHandler != null) {
            openHandler.handle(future(this, getRemoteCondition()));
        }
    }

    void fireRemoteClose() {
        if (closeHandler != null) {
            closeHandler.handle(future(this, getRemoteCondition()));
        }
    }


}
